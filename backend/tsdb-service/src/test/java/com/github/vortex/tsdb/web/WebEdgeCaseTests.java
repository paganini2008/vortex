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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import com.chaconneai.openspreader.cache.ProcessingCacheException;
import com.chaconneai.spreader.Node;
import com.chaconneai.spreader.NodeState;
import com.github.vortex.tsdb.core.TsdStoreProperties;

class WebEdgeCaseTests {

    private final ApiResultAdvice advice = new ApiResultAdvice();

    @Test
    void stampsPathAndElapsedOnlyWhenItCan() {
        assertThat(advice.beforeBodyWrite(null, null, null, null, null, null)).isNull();

        ApiResult<String> fromElsewhere = ApiResult.ok("x");
        advice.beforeBodyWrite(fromElsewhere, null, null, null, mock(ServerHttpRequest.class), null);
        assertThat(fromElsewhere.getRequestPath()).isNull();

        MockHttpServletRequest unstamped = new MockHttpServletRequest("GET", "/tsd/series");
        ApiResult<String> noStart = ApiResult.ok("x");
        advice.beforeBodyWrite(noStart, null, null, null, new ServletServerHttpRequest(unstamped), null);
        assertThat(noStart.getRequestPath()).isEqualTo("/tsd/series");
        assertThat(noStart.getElapsed()).isZero();
    }

    @Test
    void cacheFailuresBecomeFailureResults() {
        ApiResult<Void> rs = advice.handleCacheUnavailable(new ProcessingCacheException("no leader"));
        assertThat(rs.getCode()).isEqualTo(ApiResult.FAILURE);
        assertThat(rs.getMsg()).contains("no leader");
    }

    @Test
    void aNodeIsNotTheLeaderWhenThereIsNone() {
        Node node = new Node("id-1", "svc", "10.0.0.1", 22000, 0L, 0L, NodeState.ALIVE, 0, Map.of());
        ClusterController.NodeView view = ClusterController.NodeView.of(node, node, null);
        assertThat(view.leader()).isFalse();
        assertThat(view.self()).isTrue();
        assertThat(view.serverPort()).isNull();
    }

    @Test
    void noCorsMappingWithoutOrigins() {
        TsdStoreProperties props = new TsdStoreProperties();
        props.setCorsAllowedOrigins(List.of());
        CorsRegistry registry = mock(CorsRegistry.class);
        new WebMvcConfig(props).addCorsMappings(registry);
        verify(registry, never()).addMapping(anyString());
    }
}
