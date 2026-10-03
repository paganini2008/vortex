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

import static org.assertj.core.api.Assertions.assertThat;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import com.chaconneai.openspreader.cache.CacheService;
import com.chaconneai.openspreader.cache.ProcessingCache;

/**
 * The cache spilling into a real Redis: beyond max-keys, keys move to Redis and still read back.
 *
 * <p>Runs against the Redis at {@code VORTEX_TEST_REDIS_HOST}:{@code VORTEX_TEST_REDIS_PORT}
 * (localhost:6379 by default, password {@code VORTEX_TEST_REDIS_PASSWORD}), and is skipped when
 * none answers. Every key goes under a prefix of its own, removed afterwards.
 */
@EnabledIf("redisAvailable")
@SpringBootTest(properties = {"spring.spreader.name=vortex-tsd-redis-test", "spring.spreader.port=22951",
        "spring.spreader.await-join-timeout-seconds=2",
        "spring.spreader.multiprocessing.cache.max-keys=50",
        "spring.spreader.multiprocessing.cache.persistent=false",
        // Inline rather than dynamic: the Redis switch reads the host before dynamic properties exist
        "vortex.redis.host=${VORTEX_TEST_REDIS_HOST:localhost}",
        "spring.data.redis.host=${VORTEX_TEST_REDIS_HOST:localhost}",
        "spring.data.redis.port=${VORTEX_TEST_REDIS_PORT:6379}",
        "spring.data.redis.password=${VORTEX_TEST_REDIS_PASSWORD:123456}"})
class RedisOverflowTests {

    static final String HOST = env("VORTEX_TEST_REDIS_HOST", "localhost");
    static final int PORT = Integer.parseInt(env("VORTEX_TEST_REDIS_PORT", "6379"));
    static final String PREFIX = "vortex-test:" + UUID.randomUUID() + ":";

    private static RedisConnectionFactory factory;
    private static ProcessingCache cacheToClear;

    @Autowired
    private TsdStoreService store;

    @Autowired
    private ProcessingCache cache;

    @Autowired
    private CacheService cacheService;

    @Autowired
    private RedisConnectionFactory redis;

    static String env(String name, String fallback) {
        String v = System.getenv(name);
        return v == null || v.isBlank() ? fallback : v;
    }

    static boolean redisAvailable() {
        try (Socket s = new Socket()) {
            s.connect(new InetSocketAddress(HOST, PORT), 500);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @DynamicPropertySource
    static void redis(DynamicPropertyRegistry registry) {
        registry.add("spring.spreader.multiprocessing.cache.external.key-prefix", () -> PREFIX);
    }

    @AfterAll
    static void cleanUp() {
        if (factory == null) {
            return;
        }
        // Empty the cache first, so nothing is left in memory for eviction to spill after this;
        // a clear also removes the keys already under this test's prefix in Redis
        cacheToClear.clear();
        try (var conn = factory.getConnection()) {
            var keys = conn.keyCommands().scan(
                    org.springframework.data.redis.core.ScanOptions.scanOptions().match(PREFIX + "*").build());
            while (keys.hasNext()) {
                conn.keyCommands().del(keys.next());
            }
        }
    }

    @Test
    void keysBeyondTheLimitMoveToRedisAndStillReadBack() throws Exception {
        factory = redis;
        cacheToClear = cache;
        assertThat(cacheService.stats()).containsEntry("externalStore", true);
        long now = System.currentTimeMillis();
        for (int i = 0; i < 120; i++) {
            store.store(SeriesKey.of("long", "spill", "d" + i), i, now);
        }
        // Eviction runs on the cache's maintenance cycle
        long deadline = System.nanoTime() + Duration.ofSeconds(15).toNanos();
        Map<String, Object> stats = cacheService.stats();
        while (((Number) stats.get("spilled")).longValue() == 0 && System.nanoTime() < deadline) {
            Thread.sleep(200);
            stats = cacheService.stats();
        }
        assertThat(((Number) stats.get("spilled")).longValue()).isPositive();
        assertThat(((Number) stats.get("spillFailures")).longValue()).isZero();
        assertThat(((Number) stats.get("localKeyCount")).longValue()).isLessThanOrEqualTo(50);

        try (var conn = redis.getConnection()) {
            var spilled = conn.keyCommands().scan(
                    org.springframework.data.redis.core.ScanOptions.scanOptions().match(PREFIX + "tsd:*").build());
            assertThat(spilled.hasNext()).isTrue();
        }
        // Every series reads back, wherever its bucket now lives
        for (int i = 0; i < 120; i++) {
            String key = SeriesKey.of("long", "spill", "d" + i).bucketKey(store.bucketOf(now));
            assertThat(cache.stats(key).count()).as("series d%d", i).isEqualTo(1);
            assertThat(cache.stats(key).max()).isEqualTo(i);
        }
    }
}
