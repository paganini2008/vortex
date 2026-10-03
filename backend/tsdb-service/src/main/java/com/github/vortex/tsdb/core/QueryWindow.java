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

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.github.vortex.tsdb.timeseries.TimeWindowUnit;

/**
 * A validated range query: the last {@code range}, one point every {@code step} minutes, each
 * point aggregating the {@code window} minutes that end with its step.
 *
 * <ul>
 * <li><b>Tumbling</b>: {@code window == step}. Windows tile the range without overlapping:
 * every bucket is counted exactly once.</li>
 * <li><b>Sliding</b>: {@code window > step}. Consecutive windows overlap, giving a moving
 * aggregate: {@code window=15, step=1} is the last 15 minutes, recomputed every minute.</li>
 * </ul>
 *
 * <p>Steps are aligned in {@code zone}, so hourly steps fall on the local hour. A step must be a
 * whole number of buckets and tile the hour (1-30 minutes) or the day (1-12 hours), which is
 * what {@link TimeWindowUnit#locate} can align. Everything is computed from the stored buckets
 * at read time, so no window is finer than one bucket.
 *
 * @Description: QueryWindow
 * @Author: Fred Feng
 * @Date: 03/10/2026
 * @Version 2.0.0
 */
public record QueryWindow(Duration range, int stepMinutes, int windowMinutes, ZoneId zone) {

    /** The most points one query may return. */
    public static final int MAX_POINTS = 1440;

    /** The most bucket reads one query may make: points times buckets per window. */
    public static final long MAX_BUCKET_READS = 500_000;

    private static final Pattern DURATION = Pattern.compile("^(\\d+)\\s*([mhd])$");

    /**
     * One point's window, {@code [from, to)} in epoch milliseconds.
     *
     * @param slot where the point sits on the time axis: the start of its step. For a tumbling
     *             window this equals {@code from}
     */
    public record Window(long slot, long from, long to) {
    }

    public static QueryWindow of(String range, Integer step, ZoneId zone,
            TsdStoreProperties properties) {
        return of(range, step, null, zone, properties);
    }

    public static QueryWindow of(String range, Integer step, Integer window, ZoneId zone,
            TsdStoreProperties properties) {
        Duration r = parseRange(range == null || range.isBlank() ? "1h" : range);
        int span = properties.getSpan();
        int s = step != null ? step : defaultStep(r, span);
        if (s <= 0 || s % span != 0) {
            throw new IllegalArgumentException(
                    "step must be a positive multiple of the bucket span (" + span + " min)");
        }
        boolean tilesHour = s < 60 && TimeWindowUnit.MINUTES.isValidSpan(s);
        boolean tilesDay = s >= 60 && s % 60 == 0 && TimeWindowUnit.HOURS.isValidSpan(s / 60);
        if (!tilesHour && !tilesDay) {
            throw new IllegalArgumentException("step must divide an hour (1-30 min) or a day "
                    + "(60, 120, 180, 240, 360, 480 or 720 min), got " + s);
        }
        int w = window != null ? window : s;
        if (w < s || w % span != 0) {
            throw new IllegalArgumentException("window must be a multiple of the bucket span ("
                    + span + " min) and at least the step (" + s + " min), got " + w);
        }
        Duration limit = properties.getRetention();
        if (r.compareTo(limit) > 0 || Duration.ofMinutes(w).compareTo(limit) > 0) {
            throw new IllegalArgumentException(
                    "range and window must be within the retention of " + limit.toHours() + "h");
        }
        if (r.toMinutes() % s != 0) {
            throw new IllegalArgumentException("range must be a whole number of steps");
        }
        long points = r.toMinutes() / s;
        if (points > MAX_POINTS) {
            throw new IllegalArgumentException(
                    "range/step gives more than " + MAX_POINTS + " points; use a larger step");
        }
        if (points * (w / span) > MAX_BUCKET_READS) {
            throw new IllegalArgumentException("window too wide for this many points");
        }
        return new QueryWindow(r, s, w, zone);
    }

    public boolean isSliding() {
        return windowMinutes > stepMinutes;
    }

    /**
     * The windows of the range, oldest first. The last one contains {@code now} and is still
     * filling.
     *
     * <p>Only the last step is aligned in local time; the earlier ones step back in absolute
     * time. Stepping back in local time would produce a duplicate window across a daylight
     * saving change (the local hour that does not exist maps onto the next one).
     */
    public List<Window> windows(long now) {
        TimeWindowUnit unit = stepMinutes < 60 ? TimeWindowUnit.MINUTES : TimeWindowUnit.HOURS;
        int unitSpan = stepMinutes < 60 ? stepMinutes : stepMinutes / 60;
        long lastSlot = unit.locate(Instant.ofEpochMilli(now), zone, unitSpan).atZone(zone)
                .toInstant().toEpochMilli();
        long step = stepMinutes * 60_000L;
        long width = windowMinutes * 60_000L;
        int n = (int) (range.toMinutes() / stepMinutes);
        List<Window> windows = new ArrayList<>(n);
        for (int i = n - 1; i >= 0; i--) {
            long slot = lastSlot - i * step;
            long end = slot + step;
            windows.add(new Window(slot, end - width, end));
        }
        return windows;
    }

    public String rangeText() {
        long minutes = range.toMinutes();
        return minutes % 1440 == 0 ? minutes / 1440 + "d"
                : minutes % 60 == 0 ? minutes / 60 + "h" : minutes + "m";
    }

    /** About 60 points whatever the range, rounded to a step that aligns. */
    static int defaultStep(Duration range, int span) {
        int[] candidates = {1, 2, 5, 10, 15, 30, 60, 120, 180, 240, 360, 720};
        long target = Math.max(1, range.toMinutes() / 60);
        for (int c : candidates) {
            if (c >= target && c % span == 0) {
                return c;
            }
        }
        return 720;
    }

    static Duration parseRange(String text) {
        Matcher m = DURATION.matcher(text.trim().toLowerCase(Locale.ROOT));
        if (!m.matches() || Long.parseLong(m.group(1)) <= 0) {
            throw new IllegalArgumentException(
                    "range must look like 30m, 6h or 1d, got '" + text + "'");
        }
        long n = Long.parseLong(m.group(1));
        return switch (m.group(2)) {
            case "m" -> Duration.ofMinutes(n);
            case "h" -> Duration.ofHours(n);
            default -> Duration.ofDays(n);
        };
    }
}
