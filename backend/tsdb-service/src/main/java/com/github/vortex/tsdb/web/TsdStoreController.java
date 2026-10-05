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

import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.github.vortex.tsdb.core.CategoryInfo;
import com.github.vortex.tsdb.core.LastValue;
import com.github.vortex.tsdb.core.QueryWindow;
import com.github.vortex.tsdb.core.SeriesInfo;
import com.github.vortex.tsdb.core.SeriesSnapshot;
import com.github.vortex.tsdb.core.SeriesKey;
import com.github.vortex.tsdb.core.TsdQueryVo;
import com.github.vortex.tsdb.core.TsdStoreProperties;
import com.github.vortex.tsdb.core.TsdStoreService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * The API of the previous version, same paths and parameters. Any node of the cluster may be
 * called: a push is replicated to every node, and a retrieve reads the local replica.
 *
 * @Description: TsdStoreController
 * @Author: Fred Feng
 * @Date: 02/01/2025
 * @Version 1.0.0
 */
@Tag(name = "Time series", description = "Push samples and read per-minute aggregates")
@RequestMapping("/tsd")
@RestController
public class TsdStoreController {

    private final TsdStoreService tsdStoreService;
    private final TsdStoreProperties properties;

    public TsdStoreController(TsdStoreService tsdStoreService, TsdStoreProperties properties) {
        this.tsdStoreService = tsdStoreService;
        this.properties = properties;
    }

    @Operation(summary = "Push one sample",
            description = "Records the value in the current minute's bucket of the series.")
    @PostMapping("/push")
    public ApiResult<Map<String, Object>> push(
            @Parameter(description = "Data type: long, double or decimal", example = "long") @RequestParam("t") String dataType,
            @Parameter(description = "Category, e.g. a device kind or module", example = "car") @RequestParam("c") String category,
            @Parameter(description = "Dimension, e.g. a metric name", example = "speed") @RequestParam("d") String dimension,
            @Parameter(description = "The value; a long series takes integers only", example = "44") @RequestParam("v") String value) {
        SeriesKey series = SeriesKey.of(dataType, category, dimension);
        return ApiResult.ok(store(series, series.dataType().parse(value), value));
    }

    @Operation(summary = "Push a random sample", description = "Pushes a random value in [1, 10000).")
    @PostMapping("/test")
    public ApiResult<Map<String, Object>> test(
            @Parameter(description = "Data type: long, double or decimal", example = "long") @RequestParam("t") String dataType,
            @Parameter(description = "Category, e.g. a device kind or module", example = "car") @RequestParam("c") String category,
            @Parameter(description = "Dimension, e.g. a metric name", example = "speed") @RequestParam("d") String dimension) {
        SeriesKey series = SeriesKey.of(dataType, category, dimension);
        long value = ThreadLocalRandom.current().nextLong(1, 10000);
        return ApiResult.ok(store(series, value, value));
    }

    @Operation(summary = "Read the latest buckets",
            description = "Returns the last display-size buckets (60 by default), oldest first, keyed "
                    + "by bucket start as HH:mm:ss in the requested zone. Empty buckets have a count "
                    + "of 0 and null values.")
    @GetMapping("/retrieve")
    public ApiResult<TsdQueryVo> retrieve(
            @Parameter(description = "Data type: long, double or decimal", example = "long") @RequestParam("t") String dataType,
            @Parameter(description = "Category, e.g. a device kind or module", example = "car") @RequestParam("c") String category,
            @Parameter(description = "Dimension, e.g. a metric name", example = "speed") @RequestParam("d") String dimension,
            @Parameter(description = "Time zone for the labels; defaults to vortex.tsd.default-time-zone (UTC)", example = "Asia/Shanghai") @RequestParam(name = "z", required = false) String zone) {
        SeriesKey series = SeriesKey.of(dataType, category, dimension);
        Map<String, Object> data = tsdStoreService.retrieve(series, zoneOf(zone));
        return ApiResult.ok(new TsdQueryVo(series.dataType().code(), series.category(),
                series.dimension(), data));
    }

    @Operation(summary = "Read the instant value",
            description = "The latest sample of the series and when it arrived; data is null when "
                    + "the series has none within the retention period.")
    @GetMapping("/last")
    public ApiResult<LastValue> last(
            @Parameter(description = "Data type: long, double or decimal", example = "long") @RequestParam("t") String dataType,
            @Parameter(description = "Category, e.g. a device kind or module", example = "car") @RequestParam("c") String category,
            @Parameter(description = "Dimension, e.g. a metric name", example = "speed") @RequestParam("d") String dimension) {
        return ApiResult.ok(tsdStoreService.last(SeriesKey.of(dataType, category, dimension)));
    }

    @Operation(summary = "Query a range",
            description = "The last `range`, one point every `step` minutes (aligned in zone `z`), each "
                    + "point folding the `window` minutes that end with its step: highest of highest, "
                    + "lowest of lowest, totals and counts added. `window` = `step` (the default) gives "
                    + "tumbling windows; a larger `window` gives sliding ones, e.g. window=15&step=1 "
                    + "for a 15-minute moving aggregate. Also returns the instant value, the bucket "
                    + "now filling, and the whole range folded into one summary.")
    @GetMapping("/query")
    public ApiResult<SeriesSnapshot> query(
            @Parameter(description = "Data type: long, double or decimal", example = "long") @RequestParam("t") String dataType,
            @Parameter(description = "Category, e.g. a device kind or module", example = "car") @RequestParam("c") String category,
            @Parameter(description = "Dimension, e.g. a metric name", example = "speed") @RequestParam("d") String dimension,
            @Parameter(description = "How far back: 30m, 6h, 1d... at most the retention", example = "6h") @RequestParam(name = "range", required = false) String range,
            @Parameter(description = "Minutes between points; defaults to about 60 points over the range", example = "5") @RequestParam(name = "step", required = false) Integer step,
            @Parameter(description = "Minutes each point aggregates; defaults to step (tumbling), larger slides", example = "15") @RequestParam(name = "window", required = false) Integer windowSize,
            @Parameter(description = "Time zone windows are aligned in; defaults to UTC", example = "Asia/Shanghai") @RequestParam(name = "z", required = false) String zone) {
        SeriesKey series = SeriesKey.of(dataType, category, dimension);
        return ApiResult.ok(tsdStoreService.snapshot(series,
                QueryWindow.of(range, step, windowSize, zoneOf(zone), properties)));
    }

    @Operation(summary = "Query a whole category",
            description = "What /tsd/query returns, for every series in the category at once, "
                    + "ordered by dimension.")
    @GetMapping("/category")
    public ApiResult<List<SeriesSnapshot>> category(
            @Parameter(description = "Category", example = "car") @RequestParam("c") String category,
            @Parameter(description = "How far back: 30m, 6h, 1d... at most the retention", example = "1h") @RequestParam(name = "range", required = false) String range,
            @Parameter(description = "Minutes between points; defaults to about 60 points over the range") @RequestParam(name = "step", required = false) Integer step,
            @Parameter(description = "Minutes each point aggregates; defaults to step (tumbling), larger slides", example = "15") @RequestParam(name = "window", required = false) Integer windowSize,
            @Parameter(description = "Time zone windows are aligned in; defaults to UTC", example = "Asia/Shanghai") @RequestParam(name = "z", required = false) String zone) {
        return ApiResult.ok(tsdStoreService.category(SeriesKey.checkCategory(category),
                QueryWindow.of(range, step, windowSize, zoneOf(zone), properties)));
    }

    @Operation(summary = "List categories, busiest first",
            description = "Every category with its series count, its last sample, and its samples per minute "
                    + "over the last 5 complete buckets.")
    @GetMapping("/categories")
    public ApiResult<List<CategoryInfo>> categories() {
        return ApiResult.ok(tsdStoreService.categories());
    }

    @Operation(summary = "List series",
            description = "The series that received samples within the retention period, most recently active first.")
    @GetMapping("/series")
    public ApiResult<List<SeriesInfo>> series() {
        return ApiResult.ok(tsdStoreService.series());
    }

    private Map<String, Object> store(SeriesKey series, double value, Object rawValue) {
        long timestamp = System.currentTimeMillis();
        tsdStoreService.store(series, value, timestamp);
        Map<String, Object> receipt = new LinkedHashMap<>();
        receipt.put("dataType", series.dataType().code());
        receipt.put("category", series.category());
        receipt.put("dimension", series.dimension());
        receipt.put("value", rawValue);
        receipt.put("timestamp", timestamp);
        return receipt;
    }

    private ZoneId zoneOf(String zone) {
        try {
            return ZoneId.of(StringUtils.hasText(zone) ? zone.trim()
                    : properties.getDefaultTimeZone());
        } catch (DateTimeException e) {
            throw new IllegalArgumentException("Unknown time zone: " + zone);
        }
    }
}
