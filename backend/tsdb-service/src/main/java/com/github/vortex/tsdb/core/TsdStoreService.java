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

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Service;
import com.chaconneai.openspreader.cache.ProcessingCache;
import com.chaconneai.openspreader.cache.ScoredMember;
import com.github.vortex.tsdb.timeseries.NumberMetric;
import com.github.vortex.tsdb.timeseries.TimeWindowUnit;
import lombok.extern.slf4j.Slf4j;

/**
 * The time series primitives, on top of the openspreader cluster cache.
 *
 * <h2>Layout</h2>
 * Each bucket of each series is one statistical aggregate key,
 * {@code tsd:<type>:<category>:<dimension>:<bucketStartMillis>}. A sample is recorded with
 * {@code max}, {@code min} and {@code sum} on that key; {@code sum} counts the sample as it
 * goes, so the key holds exactly max, min, sum and count no matter how many samples arrive.
 *
 * <p>Writes go through the cluster leader and are replicated as operations, so every node
 * holds every bucket: a sample pushed to one node is readable on all of them a few
 * milliseconds later, and a retrieve is a run of local map lookups. This replaces the old
 * design's per-node in-memory windows plus Redis overflow plus NIO forwarding.
 *
 * <h2>Expiry</h2>
 * A bucket key is given a TTL the first time any node sees it without one, so retention
 * needs no sweeper and a writer crashing before it set the TTL is repaired by the next writer.
 *
 * <h2>Instant values</h2>
 * {@code tsd:last:<category>} is a hash holding the latest sample of each series in the
 * category, so a dashboard reads a whole category's instant values in one local lookup. It is
 * the fourth write per sample.
 *
 * <h2>Range queries</h2>
 * Tumbling and sliding windows (see {@link QueryWindow}) are folded from the stored buckets at
 * read time with {@link NumberMetric#merge}: max of maxes, min of mins, sums and counts added.
 * Nothing extra is written for them, and every read is local.
 *
 * <h2>Catalog</h2>
 * {@code tsd:catalog} is a sorted set of the series that exist, scored by when a sample last
 * arrived. It is what the UI lists. Each node refreshes an entry at most once per
 * {@link TsdStoreProperties#getCatalogTouchInterval()}.
 *
 * @Description: TsdStoreService
 * @Author: Fred Feng
 * @Date: 02/01/2025
 * @Version 1.0.0
 */
@Slf4j
@Service
public class TsdStoreService {

    public static final String CATALOG_KEY = SeriesKey.KEY_PREFIX + "catalog";

    /** How many complete buckets "busy" is measured over. */
    public static final int BUSY_WINDOW_BUCKETS = 5;

    /** The label format the previous version used for every bucket. */
    public static final DateTimeFormatter DEFAULT_DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm:ss");

    private final ProcessingCache cache;
    private final TsdStoreProperties properties;
    private final Map<SeriesKey, Long> catalogTouches = new ConcurrentHashMap<>();

    public TsdStoreService(ProcessingCache cache, TsdStoreProperties properties) {
        if (!TimeWindowUnit.MINUTES.isValidSpan(properties.getSpan())) {
            throw new IllegalStateException(
                    "vortex.tsd.span must divide 60, got " + properties.getSpan());
        }
        this.cache = cache;
        this.properties = properties;
    }

    /**
     * Records one sample.
     *
     * @return the start of the bucket the sample landed in
     */
    public long store(SeriesKey series, double value, long timestamp) {
        long bucket = bucketOf(timestamp);
        String key = series.bucketKey(bucket);
        // ttl() is a local read. -2 (absent) and -1 (no expiry) both mean nobody has set the
        // TTL yet, so this node does it after writing
        boolean needsTtl = cache.ttl(key) < 0;
        cache.max(key, value);
        cache.min(key, value);
        cache.sum(key, value);
        if (needsTtl) {
            long expiresAt = bucket + properties.spanMillis() + properties.getRetention().toMillis();
            cache.expire(key, Math.max(1, expiresAt - System.currentTimeMillis()),
                    TimeUnit.MILLISECONDS);
        }
        storeLastValue(series, value, timestamp);
        touchCatalog(series);
        return bucket;
    }

    /**
     * The latest {@code displaySize} buckets, ending with the current one, oldest first.
     * Buckets without samples are present with a count of 0 and null values.
     */
    public Map<String, Object> retrieve(SeriesKey series, ZoneId zone) {
        long span = properties.spanMillis();
        long last = bucketOf(System.currentTimeMillis());
        int size = properties.getDisplaySize();
        Map<String, Object> results = new LinkedHashMap<>();
        for (int i = size - 1; i >= 0; i--) {
            long bucket = last - i * span;
            String label = Instant.ofEpochMilli(bucket).atZone(zone)
                    .format(DEFAULT_DATE_TIME_FORMATTER);
            results.put(label, series.dataType()
                    .toMetric(cache.stats(series.bucketKey(bucket)), bucket).represent());
        }
        return results;
    }

    /** The latest sample of a series, or null when it has none within the retention period. */
    public LastValue last(SeriesKey series) {
        return LastValue.decode(series.dataType(),
                cache.hget(series.lastValueKey(), series.lastValueField()));
    }

    /** Everything a dashboard panel shows for one series. */
    public SeriesSnapshot snapshot(SeriesKey series, QueryWindow window) {
        Double seen = cache.zscore(CATALOG_KEY, series.catalogMember());
        return snapshot(series, window, last(series), seen == null ? null : seen.longValue());
    }

    /**
     * A snapshot of every series in a category, ordered by data type then dimension, so a
     * dashboard keeps its layout from one refresh to the next.
     */
    public List<SeriesSnapshot> category(String category, QueryWindow window) {
        Map<String, byte[]> lastValues = cache.hgetAll(SeriesKey.lastValueKey(category));
        return series().stream().filter(s -> s.category().equals(category))
                .sorted(Comparator.comparing(SeriesInfo::dimension).thenComparing(SeriesInfo::dataType))
                .map(s -> {
                    SeriesKey k = SeriesKey.of(s.dataType(), s.category(), s.dimension());
                    return snapshot(k, window, LastValue.decode(k.dataType(), lastValues.get(k.lastValueField())),
                            s.lastSeen());
                })
                .toList();
    }

    /**
     * Every category, busiest first: samples per minute over the last
     * {@link #BUSY_WINDOW_BUCKETS} complete buckets, then the most recently active, then by name.
     * Reads {@link #BUSY_WINDOW_BUCKETS} buckets per series that has been active in that window,
     * and none for the others.
     */
    public List<CategoryInfo> categories() {
        long span = properties.spanMillis();
        long current = bucketOf(System.currentTimeMillis());
        Map<String, long[]> by = new LinkedHashMap<>(); // series, lastSeen, samples
        long windowStart = current - BUSY_WINDOW_BUCKETS * span;
        for (SeriesInfo s : series()) {
            SeriesKey key = SeriesKey.of(s.dataType(), s.category(), s.dimension());
            long samples = 0;
            long last = latestBucket(s.lastSeen());
            for (int i = 1; i <= BUSY_WINDOW_BUCKETS && last >= windowStart; i++) {
                long b = current - i * span;
                if (b <= last) {
                    samples += cache.stats(key.bucketKey(b)).count();
                }
            }
            long[] c = by.computeIfAbsent(s.category(), k -> new long[3]);
            c[0]++;
            c[1] = Math.max(c[1], s.lastSeen());
            c[2] += samples;
        }
        double minutes = BUSY_WINDOW_BUCKETS * properties.getSpan();
        return by.entrySet().stream()
                .map(e -> new CategoryInfo(e.getKey(), (int) e.getValue()[0], e.getValue()[1],
                        Math.round(e.getValue()[2] / minutes * 10) / 10.0))
                .sorted(Comparator.comparingDouble(CategoryInfo::samplesPerMinute).reversed()
                        .thenComparing(Comparator.comparingLong(CategoryInfo::lastSeen).reversed())
                        .thenComparing(CategoryInfo::category))
                .toList();
    }

    /** The series seen within the retention period, most recently active first. */
    public List<SeriesInfo> series() {
        long since = System.currentTimeMillis() - properties.getRetention().toMillis();
        List<SeriesInfo> results = new ArrayList<>();
        for (ScoredMember m : cache.zrevrangeByScore(CATALOG_KEY, since, Double.MAX_VALUE)) {
            try {
                SeriesKey s = SeriesKey.fromCatalogMember(m.member());
                results.add(new SeriesInfo(s.dataType().code(), s.category(), s.dimension(),
                        (long) m.score()));
            } catch (IllegalArgumentException e) {
                log.warn("Skipping malformed catalog member: {}", m.memberAsString());
            }
        }
        return results;
    }

    /**
     * Drops catalog entries whose series have had no sample within the retention period, and
     * keeps the instant value hashes in step: renews the TTL of every live category's hash and
     * removes the fields of series that are gone.
     *
     * @return how many series were dropped
     */
    public int pruneCatalog() {
        long cutoff = System.currentTimeMillis() - properties.getRetention().toMillis();
        int removed = cache.zremrangeByScore(CATALOG_KEY, Double.NEGATIVE_INFINITY, cutoff);
        Map<String, Set<String>> live = new HashMap<>();
        for (SeriesInfo s : series()) {
            live.computeIfAbsent(s.category(), c -> new HashSet<>())
                    .add(SeriesKey.of(s.dataType(), s.category(), s.dimension()).lastValueField());
        }
        live.forEach((category, fields) -> {
            String key = SeriesKey.lastValueKey(category);
            for (String field : cache.hgetAll(key).keySet()) {
                if (!fields.contains(field)) {
                    cache.hdel(key, field);
                }
            }
            if (cache.exists(key)) {
                cache.expire(key, properties.getRetention().toMillis(), TimeUnit.MILLISECONDS);
            }
        });
        return removed;
    }

    /** Buckets are aligned in UTC, so every node agrees on them whatever its own zone. */
    public long bucketOf(long timestamp) {
        return TimeWindowUnit.MINUTES.locate(Instant.ofEpochMilli(timestamp), ZoneOffset.UTC,
                properties.getSpan()).toInstant(ZoneOffset.UTC).toEpochMilli();
    }

    /**
     * The last bucket a series can have samples in, given when the catalog last saw it: the
     * catalog lags a sample by up to one touch interval. Any later bucket is certainly empty,
     * and is not read: with an external store, reading a key that exists nowhere costs a round
     * trip to it.
     */
    private long latestBucket(long lastSeen) {
        return bucketOf(lastSeen + properties.getCatalogTouchInterval().toMillis());
    }

    /**
     * @param lastSeen when the catalog last saw the series, or null when it has no entry (yet):
     *                 then every bucket is read
     */
    private SeriesSnapshot snapshot(SeriesKey series, QueryWindow window, LastValue last, Long lastSeen) {
        long now = System.currentTimeMillis();
        long bound = lastSeen == null ? Long.MAX_VALUE : latestBucket(lastSeen);
        List<QueryWindow.Window> windows = window.windows(now);
        List<Map<String, Object>> points = new ArrayList<>(windows.size());
        for (QueryWindow.Window w : windows) {
            Map<String, Object> point = new LinkedHashMap<>(represent(fold(series, w, bound)));
            point.put("from", w.from());
            point.put("to", w.to());
            points.add(point);
        }
        // Folded once over the whole range rather than from the points: sliding windows
        // overlap, and adding them up would count a bucket several times
        long rangeEnd = windows.get(windows.size() - 1).to();
        long rangeStart = windows.get(0).slot();
        NumberMetric<?> summary =
                fold(series, new QueryWindow.Window(rangeStart, rangeStart, rangeEnd), bound);
        long bucket = bucketOf(now);
        NumberMetric<?> current = bucket <= bound
                ? series.dataType().toMetric(cache.stats(series.bucketKey(bucket)), bucket)
                : series.dataType().empty(bucket);
        return new SeriesSnapshot(series.dataType().code(), series.category(),
                series.dimension(), window.rangeText(), window.stepMinutes(),
                window.windowMinutes(), last, represent(current), represent(summary), points);
    }

    /**
     * One window folded from the buckets that start inside it, stamped with its slot. Buckets
     * after {@code bound} are known to be empty and are not read.
     */
    private NumberMetric<?> fold(SeriesKey series, QueryWindow.Window w, long bound) {
        long span = properties.spanMillis();
        NumberMetric<?> metric = series.dataType().empty(w.slot());
        // A window starting off a bucket boundary (an hourly step in a +05:45 zone, say)
        // takes the buckets that start inside it
        for (long b = w.from() + Math.floorMod(-w.from(), span); b < w.to() && b <= bound; b += span) {
            metric = merge(metric,
                    series.dataType().toMetric(cache.stats(series.bucketKey(b)), w.slot()));
        }
        return metric;
    }

    /** Both sides always come from the same series, so they share a value type. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static NumberMetric<?> merge(NumberMetric<?> a, NumberMetric<?> b) {
        return (NumberMetric<?>) ((NumberMetric) a).merge((NumberMetric) b);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> represent(NumberMetric<?> metric) {
        return (Map<String, Object>) metric.represent();
    }

    private void storeLastValue(SeriesKey series, double value, long timestamp) {
        String key = series.lastValueKey();
        boolean needsTtl = cache.ttl(key) < 0;
        cache.hset(key, series.lastValueField(), LastValue.encode(value, timestamp));
        if (needsTtl) {
            // Only when the hash first appears; pruneCatalog renews it while the category lives
            cache.expire(key, properties.getRetention().toMillis(), TimeUnit.MILLISECONDS);
        }
    }

    private void touchCatalog(SeriesKey series) {
        long now = System.currentTimeMillis();
        Long last = catalogTouches.get(series);
        if (last == null || now - last >= properties.getCatalogTouchInterval().toMillis()) {
            catalogTouches.put(series, now);
            cache.zadd(CATALOG_KEY, series.catalogMember(), now);
        }
    }
}
