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

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.boot.web.server.WebServer;
import org.springframework.boot.web.server.context.WebServerInitializedEvent;

import com.chaconneai.openspreader.cache.CacheService;
import com.chaconneai.spreader.GossipCluster;
import com.chaconneai.spreader.Node;
import com.chaconneai.spreader.NodeState;

/**
 * The start-up banner informs; it must never stop a node from starting.
 */
class ReadyBannerTests {

    private static WebServerInitializedEvent event() {
        WebServer server = mock(WebServer.class);
        when(server.getPort()).thenReturn(30080);
        WebServerInitializedEvent event = mock(WebServerInitializedEvent.class);
        when(event.getWebServer()).thenReturn(server);
        return event;
    }

    private static GossipCluster cluster() {
        GossipCluster cluster = mock(GossipCluster.class);
        when(cluster.self()).thenReturn(new Node("1", "svc", "10.0.0.1", 22000, 1000L, 0L, NodeState.ALIVE, 0, Map.of()));
        when(cluster.clusterName()).thenReturn("c");
        return cluster;
    }

    @Test
    void anUnreachableOverflowStoreDoesNotStopTheStart() {
        CacheService cache = mock(CacheService.class);
        when(cache.stats()).thenThrow(new IllegalStateException("Unable to connect to Redis"));
        assertThatCode(() -> new ReadyBanner(cluster(), cache).onApplicationEvent(event())).doesNotThrowAnyException();
        verify(cache).stats();
    }

    @Test
    void warnsWithoutAnOverflowStore() {
        CacheService cache = mock(CacheService.class);
        when(cache.stats()).thenReturn(Map.of("externalStore", false, "maxKeys", 10000));
        assertThatCode(() -> new ReadyBanner(cluster(), cache).onApplicationEvent(event())).doesNotThrowAnyException();
        verify(cache).stats();
    }
}
