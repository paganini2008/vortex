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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import java.net.ServerSocket;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.web.server.ConfigurableWebServerFactory;

class RandomPortCustomizerTests {

    @Test
    void picksAFreePortInTheRange() {
        ConfigurableWebServerFactory factory = mock(ConfigurableWebServerFactory.class);
        new RandomPortCustomizer(0, "50000-60000").customize(factory);
        ArgumentCaptor<Integer> port = ArgumentCaptor.forClass(Integer.class);
        verify(factory).setPort(port.capture());
        assertThat(port.getValue()).isBetween(50000, 60000);
    }

    @Test
    void leavesAnExplicitPortAlone() {
        ConfigurableWebServerFactory factory = mock(ConfigurableWebServerFactory.class);
        new RandomPortCustomizer(30080, "50000-60000").customize(factory);
        verify(factory, never()).setPort(anyInt());
    }

    @Test
    void fallsBackToTheSystemWhenTheRangeIsFull() {
        assertThat(new RandomPortCustomizer(0, "50000-50001", p -> false).pick()).isZero();
        assertThat(new RandomPortCustomizer(0, "50005-50005", p -> true).pick()).isEqualTo(50005);
    }

    @Test
    void refusesANonsenseRange() {
        for (String bad : new String[] {"60000-50000", "0-10", "50000-70000"}) {
            assertThatThrownBy(() -> new RandomPortCustomizer(0, bad)).isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Test
    void knowsATakenPort() throws Exception {
        try (ServerSocket s = new ServerSocket(0)) {
            assertThat(RandomPortCustomizer.isFree(s.getLocalPort())).isFalse();
        }
    }
}
