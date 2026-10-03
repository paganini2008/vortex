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

import java.util.Map;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.util.StringUtils;

/**
 * Leaves Spring Boot's Redis support out entirely when no Redis is configured.
 *
 * <p>Redis is optional: {@code VORTEX_REDIS_HOST} set turns the cache's overflow on, blank turns
 * it off. Left in, Spring Boot's Redis auto-configuration would still build a connection factory
 * from that blank host and refuse to start. So without a host, its auto-configurations are
 * excluded and no Redis bean exists at all.
 *
 * <p>Runs after the configuration files (and the {@code .env} they import) are read.
 * 
 * @Description: RedisSwitch
 * @Author: Fred Feng
 * @Date: 03/10/2026
 * @Version 2.0.0
 */
public class RedisSwitch implements EnvironmentPostProcessor, Ordered {

    static final String EXCLUDED = "org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration,"
            + "org.springframework.boot.data.redis.autoconfigure.DataRedisReactiveAutoConfiguration,"
            + "org.springframework.boot.data.redis.autoconfigure.DataRedisRepositoriesAutoConfiguration";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        if (StringUtils.hasText(environment.getProperty("vortex.redis.host"))) {
            return;
        }
        String existing = environment.getProperty("spring.autoconfigure.exclude", "");
        String exclude = existing.isBlank() ? EXCLUDED : existing + "," + EXCLUDED;
        environment.getPropertySources()
                .addFirst(new MapPropertySource("vortexRedisSwitch", Map.of("spring.autoconfigure.exclude", exclude)));
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
