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
package com.github.vortex.tsdb.web;

import org.springframework.boot.web.server.context.WebServerInitializedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import java.util.Map;
import com.chaconneai.openspreader.cache.CacheService;
import com.chaconneai.spreader.GossipCluster;
import lombok.extern.slf4j.Slf4j;

/**
 * Logs where this node answers (with a random port, the default outside Docker, this line is
 * how a developer finds it), and warns when keys beyond the cache's limit would be lost rather
 * than moved to an external store.
 * 
 * @Description: ReadyBanner
 * @Author: Fred Feng
 * @Date: 03/10/2026
 * @Version 1.0.0
 */
@Slf4j
@Component
public class ReadyBanner implements ApplicationListener<WebServerInitializedEvent> {

    private final GossipCluster cluster;
    private final CacheService cacheService;

    public ReadyBanner(GossipCluster cluster, CacheService cacheService) {
        this.cluster = cluster;
        this.cacheService = cacheService;
    }

    @Override
    public void onApplicationEvent(WebServerInitializedEvent event) {
        String host = cluster.self().host();
        int port = event.getWebServer().getPort();
        log.info("Vortex TSDB node ready: http://{}:{} (API /tsd, docs /swagger-ui.html), cluster {}",
                host, port, cluster.clusterName());
        Map<String, Object> stats;
        try {
            stats = cacheService.stats();
        } catch (RuntimeException e) {
            // The overflow store is down. The node still serves everything held in memory, so it
            // starts anyway; spilling resumes once the store answers
            log.warn("The cache's overflow store (Redis) is unreachable: {}. Keys beyond the in-memory "
                    + "limit cannot be moved there, nor read back, until it answers", e.getMessage());
            return;
        }
        if (!Boolean.TRUE.equals(stats.get("externalStore"))) {
            log.warn("No external store for the cache: beyond {} keys per node the least recently used "
                    + "buckets are DELETED. Set VORTEX_REDIS_HOST to move them to Redis instead",
                    stats.get("maxKeys"));
        }
    }
}
