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
package com.github.vortex.tsdb;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class RedisSwitchTests {

    @Test
    void excludesRedisWithoutAHost() {
        MockEnvironment env = new MockEnvironment().withProperty("vortex.redis.host", "");
        new RedisSwitch().postProcessEnvironment(env, null);
        assertThat(env.getProperty("spring.autoconfigure.exclude")).isEqualTo(RedisSwitch.EXCLUDED);
    }

    @Test
    void keepsOtherExclusions() {
        MockEnvironment env = new MockEnvironment().withProperty("spring.autoconfigure.exclude", "a.B");
        new RedisSwitch().postProcessEnvironment(env, null);
        assertThat(env.getProperty("spring.autoconfigure.exclude")).isEqualTo("a.B," + RedisSwitch.EXCLUDED);
    }

    @Test
    void leavesRedisInWithAHost() {
        MockEnvironment env = new MockEnvironment().withProperty("vortex.redis.host", "redis");
        new RedisSwitch().postProcessEnvironment(env, null);
        assertThat(env.getProperty("spring.autoconfigure.exclude")).isNull();
        assertThat(new RedisSwitch().getOrder()).isEqualTo(Integer.MAX_VALUE);
    }
}
