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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import com.chaconneai.openspreader.metrics.SpreaderMetricsEndpoint;
import org.springframework.beans.factory.ObjectProvider;
import com.chaconneai.openspreader.cache.ProcessingCache;
import com.chaconneai.openspreader.cache.ProcessingCacheException;
import com.chaconneai.openspreader.cluster.WebAddressMetadataListener;
import com.chaconneai.spreader.GossipCluster;
import com.chaconneai.spreader.Node;
import com.chaconneai.spreader.NodeState;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import tools.jackson.databind.json.JsonMapper;

class HealthTests {

    private static Node node(String id, String host, String port) {
        Map<String, String> meta = port == null ? Map.of() : Map.of(WebAddressMetadataListener.SERVER_PORT, port);
        return new Node(id, "svc", host, 22000, 1000L, 0L, NodeState.ALIVE, 0, meta);
    }

    private static InstanceHealth peer(String id, boolean leader, long version) {
        return peer(id, leader, version, System.currentTimeMillis());
    }

    private static InstanceHealth peer(String id, boolean leader, long version, long reportedAt) {
        return new InstanceHealth(id, "10.0.0." + id, "30080", "ALIVE", 0, leader, reportedAt, true, null,
                spreader(version), HttpRateTracker.HttpRates.NONE, null);
    }

    /** The shape of openspreader's actuator snapshot, as much of it as these tests need */
    private static Map<String, Object> spreader(long version) {
        return Map.of("components", Map.of("cache", Map.of("appliedVersion", version, "keyCount", 7)),
                "timestamp", 1L);
    }

    @Test
    void httpRatesCountOnlyTheApiAndSplitErrors() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        Timer ok = Timer.builder("http.server.requests").tag("uri", "/tsd/push").tag("outcome", "SUCCESS")
                .register(registry);
        Timer bad = Timer.builder("http.server.requests").tag("uri", "/tsd/push").tag("outcome", "CLIENT_ERROR")
                .register(registry);
        Timer boom = Timer.builder("http.server.requests").tag("uri", "/tsd/query").tag("outcome", "SERVER_ERROR")
                .register(registry);
        Timer health = Timer.builder("http.server.requests").tag("uri", "/tsd/health").tag("outcome", "SUCCESS")
                .register(registry);
        Timer actuator = Timer.builder("http.server.requests").tag("uri", "/actuator/health")
                .tag("outcome", "SUCCESS").register(registry);
        Timer.builder("http.server.requests").tag("outcome", "SUCCESS").register(registry).record(Duration.ofMillis(1));

        HttpRateTracker tracker = new HttpRateTracker(registry);
        assertThat(tracker.current()).isEqualTo(HttpRateTracker.HttpRates.NONE);
        HttpRateTracker.Totals before = tracker.read(0);
        for (int i = 0; i < 16; i++) {
            ok.record(Duration.ofMillis(10));
        }
        bad.record(Duration.ofMillis(10));
        boom.record(Duration.ofMillis(10));
        health.record(Duration.ofMillis(10));
        actuator.record(Duration.ofMillis(10));
        HttpRateTracker.HttpRates rates = HttpRateTracker.rates(before, tracker.read(2_000));
        assertThat(rates.qps()).isEqualTo(9.0);
        assertThat(rates.errorRate()).isEqualTo(0.111);
        assertThat(rates.serverErrorRate()).isEqualTo(0.056);
        assertThat(rates.avgLatencyMs()).isEqualTo(10.0);
        assertThat(rates.requests()).isEqualTo(18);

        tracker.sample();
        tracker.sample();
        assertThat(tracker.current().requests()).isEqualTo(18);
        assertThat(HttpRateTracker.rates(before, before).errorRate()).isZero();
    }

    private final JsonMapper json = JsonMapper.builder().build();

    @SuppressWarnings("unchecked")
    private static ObjectProvider<SpreaderMetricsEndpoint> endpoint(Map<String, Object> snapshot) {
        ObjectProvider<SpreaderMetricsEndpoint> provider = mock(ObjectProvider.class);
        if (snapshot != null) {
            SpreaderMetricsEndpoint e = mock(SpreaderMetricsEndpoint.class);
            when(e.snapshot()).thenReturn(snapshot);
            when(provider.getIfAvailable()).thenReturn(e);
        }
        return provider;
    }

    @SuppressWarnings("unchecked")
    private static JvmSampler jvm() {
        return new JvmSampler(new SimpleMeterRegistry(), mock(org.springframework.beans.factory.ObjectProvider.class));
    }

    private byte[] report(InstanceHealth h) {
        return json.writeValueAsBytes(h);
    }

    @Test
    void readsEveryInstanceFromTheCacheAndFlagsTheSilent() {
        GossipCluster cluster = mock(GossipCluster.class);
        ProcessingCache cache = mock(ProcessingCache.class);
        Node self = node("1", "10.0.0.1", "30080");
        Node fresh = node("2", "10.0.0.2", "30080");
        Node stale = node("3", "10.0.0.3", "30080");
        Node silent = node("4", "10.0.0.4", null);
        Node garbled = node("5", "10.0.0.5", "30080");
        when(cluster.self()).thenReturn(self);
        when(cluster.leader()).thenReturn(self);
        when(cluster.isLeader()).thenReturn(true);
        when(cluster.clusterName()).thenReturn("c");
        when(cluster.members()).thenReturn(List.of(stale, silent, self, fresh, garbled));
        long now = System.currentTimeMillis();
        InstanceHealth freshReport = peer("2", false, 97, now - 1_000);
        InstanceHealth staleReport = peer("3", false, 50, now - 60_000);
        when(cache.hgetAll(HealthService.HEALTH_KEY)).thenReturn(Map.of(
                "2", report(freshReport), "3", report(staleReport), "5", "{oops".getBytes()));

        HealthService service = new HealthService(cluster, endpoint(spreader(100)), cache,
                new HttpRateTracker(new SimpleMeterRegistry()), jvm(), json);
        HealthService.ClusterHealth health = service.cluster();

        assertThat(health.leader()).isEqualTo("10.0.0.1");
        assertThat(health.instances()).extracting(InstanceHealth::host)
                .containsExactly("10.0.0.1", "10.0.0.2", "10.0.0.3", "10.0.0.4", "10.0.0.5");
        InstanceHealth me = health.instances().get(0);
        assertThat(me.leader()).isTrue();
        assertThat(me.appliedVersion()).isEqualTo(100);
        // openspreader's own timestamp goes; the reading has one
        assertThat(me.spreader()).containsKey("components").doesNotContainKey("timestamp");
        // Numbers in the cache map come back from JSON as the narrowest type, so compare by field
        InstanceHealth read = health.instances().get(1);
        assertThat(read.reachable()).isTrue();
        assertThat(read.reportedAt()).isEqualTo(freshReport.reportedAt());
        assertThat(read.appliedVersion()).isEqualTo(97);
        assertThat(health.instances().get(2).reachable()).isFalse();
        assertThat(health.instances().get(2).error()).startsWith("no report for 60");
        assertThat(health.instances().get(3).error()).isEqualTo("no report yet");
        assertThat(health.instances().get(4).error()).isEqualTo("unreadable report");
        // Lag compares stored readings of one round; this one has no reading from the leader
        assertThat(health.replicationLag()).isEmpty();
        assertThat(health.maxReplicationLag()).isZero();
    }

    @Test
    void anUnreachableOverflowStoreStillLeavesTheRestOfTheReading() {
        @SuppressWarnings("unchecked")
        ObjectProvider<SpreaderMetricsEndpoint> provider = mock(ObjectProvider.class);
        SpreaderMetricsEndpoint e = mock(SpreaderMetricsEndpoint.class);
        when(e.snapshot()).thenThrow(new IllegalStateException("Unable to connect to Redis"));
        when(provider.getIfAvailable()).thenReturn(e);
        HealthService service = new HealthService(mock(GossipCluster.class), provider, mock(ProcessingCache.class),
                new HttpRateTracker(new SimpleMeterRegistry()), jvm(), json);
        assertThat(service.spreaderFigures()).containsEntry("error", "Unable to connect to Redis");
    }

    @Test
    void reportsItselfAndTheLeaderDropsDepartedInstances() {
        GossipCluster cluster = mock(GossipCluster.class);
        ProcessingCache cache = mock(ProcessingCache.class);
        Node self = node("1", "10.0.0.1", "30080");
        when(cluster.self()).thenReturn(self);
        when(cluster.members()).thenReturn(List.of(self));
        when(cache.hgetAll(HealthService.HEALTH_KEY)).thenReturn(Map.of("1", new byte[0], "gone", new byte[0]));
        HealthService service = new HealthService(cluster, endpoint(null), cache,
                new HttpRateTracker(new SimpleMeterRegistry()), jvm(), json);
        assertThat(service.spreaderFigures()).isEmpty();

        service.report();
        verify(cache).hset(eq(HealthService.HEALTH_KEY), eq("1"), any(byte[].class));
        verify(cache, never()).hdel(anyString(), anyString());

        when(cluster.isLeader()).thenReturn(true);
        service.report();
        verify(cache).hdel(HealthService.HEALTH_KEY, "gone");

        // A write refused during a change of leader is skipped, not thrown
        when(cache.hset(anyString(), anyString(), any(byte[].class)))
                .thenThrow(new ProcessingCacheException("no leader"));
        service.report();
    }

    @Test
    void lagComparesOnlyReadingsOfTheSameRound() {
        long t = System.currentTimeMillis();
        Map<String, Long> lag = HealthService.lags(List.of(peer("1", true, 100, t), peer("2", false, 97, t + 200),
                peer("3", false, 120, t + 300), peer("4", false, 10, t - 4_000)));
        assertThat(lag).containsExactly(Map.entry("2", 3L), Map.entry("3", 0L));
        assertThat(HealthService.lags(List.of(peer("2", false, 12)))).isEmpty();
        assertThat(InstanceHealth.unreachable("x", "h", null, "DEAD", 0, false, 0, "gone").appliedVersion()).isZero();
    }
}
