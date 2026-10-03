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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import com.chaconneai.openspreader.cache.CacheService;
import com.chaconneai.openspreader.cache.ProcessingCache;
import com.github.vortex.tsdb.core.SeriesKey;
import com.github.vortex.tsdb.core.TsdStoreService;
import com.jayway.jsonpath.JsonPath;

/**
 * Runs against a real one-node cluster. The cluster port and name differ from the defaults so
 * a locally running instance is not joined by accident.
 */
@SpringBootTest(properties = {"spring.spreader.name=vortex-tsd-test",
        "spring.spreader.port=22950", "spring.spreader.await-join-timeout-seconds=2"})
@AutoConfigureMockMvc
class TsdStoreControllerTests {

    private static final Path DATA_DIR = createDataDir();

    private static Path createDataDir() {
        try {
            Path dir = Files.createTempDirectory("vortex-tsdb-test");
            dir.toFile().deleteOnExit();
            return dir;
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Never the developer's real data file, and never a leftover from a previous run */
    @DynamicPropertySource
    static void dataDir(DynamicPropertyRegistry registry) {
        registry.add("spring.spreader.multiprocessing.cache.persistent-file",
                () -> DATA_DIR.resolve("cache").toString());
    }

    @Autowired
    private CacheService cacheService;

    @Autowired
    private com.github.vortex.tsdb.health.HealthService healthService;

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ProcessingCache cache;

    @Autowired
    private TsdStoreService tsdStoreService;

    @Test
    void pushedSamplesAreAggregatedPerBucket() throws Exception {
        for (String v : new String[] {"10", "40", "25"}) {
            mvc.perform(post("/tsd/push").param("t", "long").param("c", "car")
                    .param("d", "speed").param("v", v)).andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(1))
                    .andExpect(jsonPath("$.data.value").value(v));
        }
        String body = mvc
                .perform(get("/tsd/retrieve").param("t", "long").param("c", "car")
                        .param("d", "speed").param("z", "Asia/Shanghai"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.requestPath").value("/tsd/retrieve"))
                .andExpect(jsonPath("$.data.dataType").value("long"))
                .andExpect(jsonPath("$.data.category").value("car"))
                .andExpect(jsonPath("$.data.dimension").value("speed"))
                .andReturn().getResponse().getContentAsString();

        Map<String, Map<String, Object>> data = JsonPath.read(body, "$.data.data");
        assertThat(data).hasSize(60);
        // The three pushes may straddle a minute boundary, so look across all buckets
        List<Map<String, Object>> filled = new ArrayList<>();
        data.values().stream().filter(m -> ((Number) m.get("count")).longValue() > 0)
                .forEach(filled::add);
        assertThat(filled).isNotEmpty();
        assertThat(filled.stream().mapToLong(m -> ((Number) m.get("count")).longValue()).sum())
                .isEqualTo(3);
        assertThat(filled.stream().mapToLong(m -> ((Number) m.get("highestValue")).longValue())
                .max().getAsLong()).isEqualTo(40);
        assertThat(filled.stream().mapToLong(m -> ((Number) m.get("lowestValue")).longValue())
                .min().getAsLong()).isEqualTo(10);
        assertThat(filled.stream().mapToLong(m -> ((Number) m.get("totalValue")).longValue())
                .sum()).isEqualTo(75);
        if (filled.size() == 1) {
            assertThat(((Number) filled.get(0).get("averageValue")).doubleValue()).isEqualTo(25.0);
        }

        // An empty bucket reports a zero count and no values
        Map<String, Object> first = data.values().iterator().next();
        assertThat(first.get("count")).isEqualTo(0);
        assertThat(first.get("highestValue")).isNull();
    }

    @Test
    void bucketKeysExpireAndTheSeriesIsCatalogued() throws Exception {
        mvc.perform(post("/tsd/test").param("t", "double").param("c", "cpu").param("d", "load"))
                .andExpect(status().isOk());
        SeriesKey series = SeriesKey.of("double", "cpu", "load");
        String key = series.bucketKey(tsdStoreService.bucketOf(System.currentTimeMillis()));
        long ttl = cache.ttl(key);
        if (ttl == -2) {
            // Pushed just before a minute boundary; the sample is in the previous bucket
            key = series.bucketKey(
                    tsdStoreService.bucketOf(System.currentTimeMillis()) - 60_000);
            ttl = cache.ttl(key);
        }
        assertThat(ttl).isGreaterThan(23 * 3_600_000L);

        mvc.perform(get("/tsd/series")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.category == 'cpu' && @.dimension == 'load')]")
                        .exists());
    }

    @Test
    void decimalSumsHideBinaryNoise() throws Exception {
        for (String v : new String[] {"0.1", "0.2"}) {
            mvc.perform(post("/tsd/push").param("t", "decimal").param("c", "pay")
                    .param("d", "amount").param("v", v)).andExpect(status().isOk());
        }
        String body = mvc.perform(get("/tsd/retrieve").param("t", "decimal").param("c", "pay")
                .param("d", "amount")).andReturn().getResponse().getContentAsString();
        List<Number> totals = JsonPath.read(body, "$.data.data.*[?(@.count > 0)].totalValue");
        assertThat(totals.stream().mapToDouble(Number::doubleValue).sum()).isEqualTo(0.3);
    }

    @Test
    void badInputIsRejectedWithTheFailureCode() throws Exception {
        mvc.perform(post("/tsd/push").param("t", "long").param("c", "car").param("d", "speed")
                .param("v", "fast")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(0));
        mvc.perform(post("/tsd/push").param("t", "float").param("c", "car").param("d", "speed")
                .param("v", "1")).andExpect(status().isBadRequest());
        mvc.perform(post("/tsd/push").param("t", "long").param("c", "car:x").param("d", "speed")
                .param("v", "1")).andExpect(status().isBadRequest());
        mvc.perform(get("/tsd/retrieve").param("t", "long").param("c", "car").param("d", "speed")
                .param("z", "Mars/Olympus")).andExpect(status().isBadRequest());
        mvc.perform(get("/tsd/retrieve").param("t", "long")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void clusterViewShowsThisNodeAsLeader() throws Exception {
        mvc.perform(get("/tsd/cluster")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.clusterName").value("vortex-tsd-test"))
                .andExpect(jsonPath("$.data.self.self").value(true))
                .andExpect(jsonPath("$.data.members.length()").value(1))
                .andExpect(jsonPath("$.data.members[0].leader").value(true));
    }

    @Test
    void openApiDocumentDescribesTheApi() throws Exception {
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("Vortex TSDB API"))
                .andExpect(jsonPath("$.paths['/tsd/push'].post").exists())
                .andExpect(jsonPath("$.paths['/tsd/retrieve'].get").exists());
        mvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
    }

    @Test
    void instantValueIsTheLatestSample() throws Exception {
        for (String v : new String[] {"3", "9", "5"}) {
            mvc.perform(post("/tsd/push").param("t", "long").param("c", "room")
                    .param("d", "people").param("v", v)).andExpect(status().isOk());
        }
        mvc.perform(get("/tsd/last").param("t", "long").param("c", "room").param("d", "people"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.value").value(5))
                .andExpect(jsonPath("$.data.timestamp").isNumber());
        mvc.perform(get("/tsd/last").param("t", "long").param("c", "room").param("d", "nobody"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data").doesNotExist());
        assertThat(cache.ttl(SeriesKey.lastValueKey("room"))).isGreaterThan(23 * 3_600_000L);
    }

    @Test
    void rangeQueryFoldsBucketsIntoWindows() throws Exception {
        for (String v : new String[] {"2", "8", "5"}) {
            mvc.perform(post("/tsd/push").param("t", "long").param("c", "fold")
                    .param("d", "x").param("v", v)).andExpect(status().isOk());
        }
        String body = mvc.perform(get("/tsd/query").param("t", "long").param("c", "fold")
                .param("d", "x").param("range", "1h").param("step", "30").param("z", "UTC"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.step").value(30))
                .andExpect(jsonPath("$.data.range").value("1h"))
                .andExpect(jsonPath("$.data.points.length()").value(2))
                .andExpect(jsonPath("$.data.summary.count").value(3))
                .andExpect(jsonPath("$.data.summary.highestValue").value(8))
                .andExpect(jsonPath("$.data.summary.lowestValue").value(2))
                .andExpect(jsonPath("$.data.summary.averageValue").value(5.0))
                .andExpect(jsonPath("$.data.last.value").value(5))
                .andReturn().getResponse().getContentAsString();
        List<Number> starts = JsonPath.read(body, "$.data.points[*].timestamp");
        assertThat(starts.get(1).longValue() - starts.get(0).longValue()).isEqualTo(1_800_000);
        assertThat(starts.get(1).longValue() % 1_800_000).isZero();

        mvc.perform(get("/tsd/query").param("t", "long").param("c", "fold").param("d", "x")
                .param("step", "7")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void categoryReturnsEverySeriesInIt() throws Exception {
        mvc.perform(post("/tsd/push").param("t", "long").param("c", "truck").param("d", "speed")
                .param("v", "70")).andExpect(status().isOk());
        mvc.perform(post("/tsd/push").param("t", "double").param("c", "truck").param("d", "fuel")
                .param("v", "0.42")).andExpect(status().isOk());
        mvc.perform(post("/tsd/push").param("t", "long").param("c", "bike").param("d", "speed")
                .param("v", "20")).andExpect(status().isOk());
        mvc.perform(get("/tsd/category").param("c", "truck").param("range", "30m"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].dimension").value("fuel"))
                .andExpect(jsonPath("$.data[0].last.value").value(0.42))
                .andExpect(jsonPath("$.data[1].dimension").value("speed"))
                .andExpect(jsonPath("$.data[1].current.count").value(1))
                .andExpect(jsonPath("$.data[1].points.length()").value(30));
        mvc.perform(get("/tsd/category").param("c", "bad:name"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void pruningRenewsInstantValuesAndDropsTheGone() throws Exception {
        mvc.perform(post("/tsd/push").param("t", "long").param("c", "lab").param("d", "temp")
                .param("v", "21")).andExpect(status().isOk());
        String key = SeriesKey.lastValueKey("lab");
        // A field whose series is not in the catalog any more
        cache.hset(key, "long|ghost", new byte[16]);
        cache.expire(key, 60_000, java.util.concurrent.TimeUnit.MILLISECONDS);
        tsdStoreService.pruneCatalog();
        assertThat(cache.hexists(key, "long|ghost")).isFalse();
        assertThat(cache.hexists(key, "long|temp")).isTrue();
        assertThat(cache.ttl(key)).isGreaterThan(23 * 3_600_000L);
    }

    @Test
    void slidingWindowsOverlapButTheSummaryCountsOnce() throws Exception {
        SeriesKey series = SeriesKey.of("long", "slide", "x");
        tsdStoreService.store(series, 10, System.currentTimeMillis() - 180_000);
        String sliding = mvc.perform(get("/tsd/query").param("t", "long").param("c", "slide")
                .param("d", "x").param("range", "10m").param("step", "1").param("window", "5"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.window").value(5))
                .andExpect(jsonPath("$.data.summary.count").value(1))
                .andReturn().getResponse().getContentAsString();
        List<Number> counts = JsonPath.read(sliding, "$.data.points[*].count");
        assertThat(counts.stream().mapToLong(Number::longValue).sum()).isEqualTo(4);
        Number from = JsonPath.read(sliding, "$.data.points[9].from");
        Number to = JsonPath.read(sliding, "$.data.points[9].to");
        assertThat(to.longValue() - from.longValue()).isEqualTo(300_000);

        String tumbling = mvc.perform(get("/tsd/query").param("t", "long").param("c", "slide")
                .param("d", "x").param("range", "10m").param("step", "1"))
                .andReturn().getResponse().getContentAsString();
        List<Number> once = JsonPath.read(tumbling, "$.data.points[*].count");
        assertThat(once.stream().mapToLong(Number::longValue).sum()).isEqualTo(1);

        mvc.perform(get("/tsd/query").param("t", "long").param("c", "slide").param("d", "x")
                .param("step", "5").param("window", "2")).andExpect(status().isBadRequest());
    }

    @Test
    void noRedisAddressMeansNoExternalStore() {
        assertThat(cacheService.stats()).containsEntry("externalStore", false);
    }

    @Test
    void snapshotWritesTheCacheToDisk() throws Exception {
        mvc.perform(post("/tsd/push").param("t", "long").param("c", "disk").param("d", "x")
                .param("v", "1")).andExpect(status().isOk());
        assertThat(cacheService.dumpToDisk()).isPositive();
        assertThat(Files.size(DATA_DIR.resolve("cache"))).isPositive();
    }

    @Test
    void healthCoversTheCacheAndTheApi() throws Exception {
        mvc.perform(get("/tsd/health/self")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.leader").value(true))
                .andExpect(jsonPath("$.data.reachable").value(true))
                .andExpect(jsonPath("$.data.spreader.components.cache.appliedVersion").isNumber())
                .andExpect(jsonPath("$.data.spreader.components.cache.outboxOverflow").value(0))
                .andExpect(jsonPath("$.data.spreader.components.mutex").exists())
                .andExpect(jsonPath("$.data.spreader.cluster.splitBrain.splitting").value(false))
                .andExpect(jsonPath("$.data.http.qps").isNumber())
                .andExpect(jsonPath("$.data.jvm.status").value("UP"))
                .andExpect(jsonPath("$.data.jvm.heapUsed").isNumber());
        healthService.report();
        mvc.perform(get("/tsd/health")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.clusterName").value("vortex-tsd-test"))
                .andExpect(jsonPath("$.data.instances.length()").value(1))
                .andExpect(jsonPath("$.data.maxReplicationLag").value(0));
    }

    @Test
    void categoriesComeBusiestFirst() throws Exception {
        long earlier = System.currentTimeMillis() - 120_000;
        SeriesKey quiet = SeriesKey.of("long", "zz-quiet", "x");
        SeriesKey busy = SeriesKey.of("long", "zz-busy", "x");
        tsdStoreService.store(quiet, 1, earlier);
        for (int i = 0; i < 10; i++) {
            tsdStoreService.store(busy, 1, earlier);
        }
        tsdStoreService.store(SeriesKey.of("double", "zz-busy", "y"), 1, earlier);
        String body = mvc.perform(get("/tsd/categories")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        List<String> names = JsonPath.read(body, "$.data[*].category");
        assertThat(names.indexOf("zz-busy")).isLessThan(names.indexOf("zz-quiet"));
        List<Number> rate = JsonPath.read(body, "$.data[?(@.category == 'zz-busy')].samplesPerMinute");
        // 11 samples in the window of 5 one-minute buckets
        assertThat(rate.get(0).doubleValue()).isEqualTo(2.2);
        List<Integer> series = JsonPath.read(body, "$.data[?(@.category == 'zz-busy')].series");
        assertThat(series).containsExactly(2);
    }

    @Test
    void bucketsAfterTheLastSampleAreNotRead() throws Exception {
        SeriesKey series = SeriesKey.of("long", "stale", "x");
        tsdStoreService.store(series, 7, System.currentTimeMillis());
        // Make the catalog believe the last sample was two hours ago: the recent bucket is then
        // known to be empty and is not read, which is what the count shows
        cache.zadd(TsdStoreService.CATALOG_KEY, series.catalogMember(), System.currentTimeMillis() - 7_200_000);
        mvc.perform(get("/tsd/query").param("t", "long").param("c", "stale").param("d", "x").param("range", "30m"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.summary.count").value(0))
                .andExpect(jsonPath("$.data.current.count").value(0));
        String body = mvc.perform(get("/tsd/categories")).andReturn().getResponse().getContentAsString();
        List<Number> rate = JsonPath.read(body, "$.data[?(@.category == 'stale')].samplesPerMinute");
        assertThat(rate.get(0).doubleValue()).isZero();
    }
}
