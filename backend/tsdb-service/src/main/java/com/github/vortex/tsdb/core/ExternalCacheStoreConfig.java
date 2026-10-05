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
package com.github.vortex.tsdb.core;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import com.chaconneai.openspreader.MultiProcessingProperties;
import com.chaconneai.openspreader.cache.CacheStore;
import com.chaconneai.openspreader.cache.RedisCacheStore;
import lombok.extern.slf4j.Slf4j;

/**
 * Declares the Redis store openspreader's cache spills into, when a Redis is configured.
 *
 * <p>The switch is the Redis address itself, {@code VORTEX_REDIS_HOST} ({@code vortex.redis.host}):
 * set, keys beyond {@code max-keys} move to that Redis; blank, there is no store and they are
 * evicted. Spring Boot's own default of localhost does not count, so nothing connects to a Redis
 * nobody asked for.
 *
 * <p>openspreader declares the same bean itself, but behind
 * {@code @ConditionalOnBean(RedisConnectionFactory.class)} in an auto-configuration that is not
 * ordered after Spring Boot's Redis one: the condition is evaluated before the factory exists, so
 * the store silently never appears and eviction keeps deleting. Declared here, as an application
 * bean, it does not depend on that ordering. openspreader's own bean is
 * {@code @ConditionalOnMissingBean(CacheStore.class)}, so the two never clash; once openspreader
 * orders itself after {@code DataRedisAutoConfiguration}, this class can go.
 * 
 * @Description: ExternalCacheStoreConfig
 * @Author: Fred Feng
 * @Date: 03/10/2026
 * @Version 1.0.0
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
@ConditionalOnExpression("'${vortex.redis.host:}' != ''")
public class ExternalCacheStoreConfig {

    @Bean
    public CacheStore externalCacheStore(RedisConnectionFactory factory, MultiProcessingProperties props) {
        String prefix = props.getCache().getExternal().getKeyPrefix();
        log.info("Cache overflow goes to Redis under the prefix \"{}\"", prefix);
        return new RedisCacheStore(factory, prefix);
    }
}
