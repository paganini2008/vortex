/*
 * Copyright 2017-2026 Fred Feng (paganini.fy@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.vortex.tsdb.health;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import com.chaconneai.openspreader.cache.ProcessingCache;
import com.chaconneai.openspreader.cluster.WebAddressMetadataListener;
import com.chaconneai.spreader.GossipCluster;
import com.chaconneai.spreader.Node;
import com.chaconneai.openspreader.metrics.SpreaderMetricsEndpoint;
import com.github.vortex.tsdb.core.SeriesKey;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.json.JsonMapper;

/**
 * Gathers the health of every instance in the cluster, through the replicated cache.
 *
 * <p>Every instance writes its own reading into the hash {@link #HEALTH_KEY} every
 * {@link #REPORT_INTERVAL_MS}, one field per instance. Whichever instance answers
 * {@code /tsd/health} then reads all of them from its own copy, as it reads any series: no call
 * between instances, nothing to time out. A reading older than {@link #STALE_AFTER_MS} means the
 * instance has stopped reporting, and it is listed as unreachable.
 * 
 * @Description: HealthService
 * @Author: Fred Feng
 * @Date: 03/10/2026
 * @Version 1.0.0
 */
@Slf4j
@Service
public class HealthService {

    public static final String HEALTH_KEY = SeriesKey.KEY_PREFIX + "health";

    static final long REPORT_INTERVAL_MS = 5_000;

    /** Three missed reports */
    static final long STALE_AFTER_MS = 3 * REPORT_INTERVAL_MS;

    /** Readings this far apart are not compared: the leader writes on between them. */
    static final long SAME_ROUND_MS = 1_000;

    /**
     * The whole cluster.
     *
     * @param replicationLag versions each follower trails the leader by, from readings taken in
     *                       the same round; a follower without one is missing
     */
    public record ClusterHealth(String clusterName, String leader, long maxReplicationLag,
            Map<String, Long> replicationLag, List<InstanceHealth> instances) {
    }

    private final GossipCluster cluster;
    private final ObjectProvider<SpreaderMetricsEndpoint> spreader;
    private final ProcessingCache cache;
    private final HttpRateTracker httpRates;
    private final JvmSampler jvm;
    private final JsonMapper json;

    public HealthService(GossipCluster cluster, ObjectProvider<SpreaderMetricsEndpoint> spreader, ProcessingCache cache,
            HttpRateTracker httpRates, JvmSampler jvm, JsonMapper json) {
        this.cluster = cluster;
        this.spreader = spreader;
        this.cache = cache;
        this.httpRates = httpRates;
        this.jvm = jvm;
        this.json = json;
    }

    public InstanceHealth self() {
        Node self = cluster.self();
        return new InstanceHealth(self.id(), self.host(), self.metadata(WebAddressMetadataListener.SERVER_PORT),
                String.valueOf(self.state()), self.startTime(), cluster.isLeader(), System.currentTimeMillis(),
                true, null, spreaderFigures(), httpRates.current(), jvm.sample());
    }

    /**
     * Writes this instance's reading; the leader also drops readings of instances that left.
     *
     * <p>On the wall clock's 5-second marks rather than every 5 seconds from start-up, so every
     * instance takes its reading at the same moment and their versions can be compared: with the
     * leader writing a thousand times a second, readings a few seconds apart differ by thousands.
     */
    @Scheduled(cron = "*/5 * * * * *")
    public void report() {
        try {
            cache.hset(HEALTH_KEY, cluster.self().id(), json.writeValueAsBytes(self()));
            if (cluster.isLeader()) {
                Set<String> members = cluster.members().stream().map(Node::id).collect(Collectors.toSet());
                for (String id : cache.hgetAll(HEALTH_KEY).keySet()) {
                    if (!members.contains(id)) {
                        cache.hdel(HEALTH_KEY, id);
                    }
                }
            }
        } catch (RuntimeException e) {
            // A write fails while leadership changes hands; the next report carries on
            log.debug("Health report skipped: {}", e.getMessage());
        }
    }

    public ClusterHealth cluster() {
        Node self = cluster.self();
        Node leader = cluster.leader();
        long now = System.currentTimeMillis();
        Map<String, byte[]> reports = cache.hgetAll(HEALTH_KEY);
        List<InstanceHealth> fromCache = cluster.members().stream()
                .map(n -> reported(n, leader, reports.get(n.id()), now)).toList();
        List<InstanceHealth> instances = cluster.members().stream()
                .map(n -> n.id().equals(self.id()) ? self()
                        : fromCache.stream().filter(h -> h.id().equals(n.id())).findFirst().orElseThrow())
                .sorted(Comparator.comparing(InstanceHealth::host).thenComparing(InstanceHealth::serverPort,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
        // Lag from the stored readings, self included, so that all of them are of one round
        Map<String, Long> lag = lags(fromCache);
        return new ClusterHealth(cluster.clusterName(), leader != null ? leader.host() : null,
                lag.values().stream().mapToLong(Long::longValue).max().orElse(0), lag, instances);
    }

    private InstanceHealth reported(Node n, Node leader, byte[] report, long now) {
        if (report == null) {
            return unreachable(n, leader, 0, "no report yet");
        }
        InstanceHealth h;
        try {
            h = json.readValue(report, InstanceHealth.class);
        } catch (RuntimeException e) {
            return unreachable(n, leader, 0, "unreadable report");
        }
        long age = now - h.reportedAt();
        if (age > STALE_AFTER_MS) {
            return unreachable(n, leader, h.reportedAt(), "no report for " + age / 1000 + "s");
        }
        return h;
    }

    private static InstanceHealth unreachable(Node n, Node leader, long reportedAt, String error) {
        return InstanceHealth.unreachable(n.id(), n.host(), n.metadata(WebAddressMetadataListener.SERVER_PORT),
                String.valueOf(n.state()), n.startTime(), leader != null && leader.id().equals(n.id()),
                reportedAt, error);
    }

    /**
     * How many versions each follower trails the leader by, comparing only readings taken in the
     * same round. A follower read a moment after the leader can appear ahead; that counts as 0.
     */
    static Map<String, Long> lags(List<InstanceHealth> readings) {
        InstanceHealth leader = readings.stream().filter(i -> i.leader() && i.reachable()).findFirst().orElse(null);
        Map<String, Long> lag = new LinkedHashMap<>();
        if (leader == null) {
            return lag;
        }
        for (InstanceHealth i : readings) {
            if (!i.leader() && i.reachable() && Math.abs(i.reportedAt() - leader.reportedAt()) <= SAME_ROUND_MS) {
                lag.put(i.id(), Math.max(0, version(leader) - version(i)));
            }
        }
        return lag;
    }

    private static long version(InstanceHealth i) {
        return i.appliedVersion();
    }

    /**
     * openspreader's actuator snapshot, as {@code /actuator/spreader} shows it, without its own
     * timestamp (the reading carries one). Empty when the actuator is not on the classpath.
     */
    Map<String, Object> spreaderFigures() {
        SpreaderMetricsEndpoint endpoint = spreader.getIfAvailable();
        if (endpoint == null) {
            return Map.of();
        }
        Map<String, Object> figures;
        try {
            figures = new LinkedHashMap<>(endpoint.snapshot());
        } catch (RuntimeException e) {
            // The cache's figures include a key count from the overflow store; when that store is
            // down the rest of the reading (API, JVM) still goes out, with the reason
            return Map.of("error", String.valueOf(e.getMessage()));
        }
        figures.remove("timestamp");
        return figures;
    }
}
