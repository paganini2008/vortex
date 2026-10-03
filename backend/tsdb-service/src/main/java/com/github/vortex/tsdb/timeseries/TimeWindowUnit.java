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
package com.github.vortex.tsdb.timeseries;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * Aligns instants to the start of their time window, in a given time zone.
 *
 * <p>Carried over from doodler-common-data, taking a {@link ZoneId} instead of a
 * {@code TimeZone}. {@code initializeMap} is gone: it converted local times back with the
 * offset in force <i>now</i>, which is wrong on the other side of a daylight saving change.
 *
 * @Description: TimeWindowUnit
 * @Author: Fred Feng
 * @Date: 17/11/2024
 * @Version 1.0.0
 */
public enum TimeWindowUnit {

    SECONDS(60) {

        @Override
        public LocalDateTime locate(Instant timestamp, ZoneId zone, int span) {
            final ZonedDateTime zdt = timestamp.atZone(zone);
            int second = zdt.getSecond();
            return LocalDateTime.of(zdt.toLocalDate(),
                    LocalTime.of(zdt.getHour(), zdt.getMinute(), second - second % span));
        }

        @Override
        public int sizeOf(int span, int days) {
            return (60 % span == 0 ? (60 / span) : (60 / span + 1)) * 60 * 24 * days;
        }
    },

    MINUTES(60) {

        @Override
        public LocalDateTime locate(Instant timestamp, ZoneId zone, int span) {
            final ZonedDateTime zdt = timestamp.atZone(zone);
            int minute = zdt.getMinute();
            return LocalDateTime.of(zdt.toLocalDate(),
                    LocalTime.of(zdt.getHour(), minute - minute % span, 0));
        }

        @Override
        public int sizeOf(int span, int days) {
            return (60 % span == 0 ? (60 / span) : (60 / span + 1)) * 24 * days;
        }
    },

    HOURS(24) {

        @Override
        public LocalDateTime locate(Instant timestamp, ZoneId zone, int span) {
            final ZonedDateTime zdt = timestamp.atZone(zone);
            int hour = zdt.getHour();
            return LocalDateTime.of(zdt.toLocalDate(), LocalTime.of(hour - hour % span, 0, 0));
        }

        @Override
        public int sizeOf(int span, int days) {
            return (24 % span == 0 ? (24 / span) : (24 / span + 1)) * days;
        }
    },

    DAYS(1) {

        @Override
        public LocalDateTime locate(Instant timestamp, ZoneId zone, int span) {
            return timestamp.atZone(zone).toLocalDate().atStartOfDay();
        }

        @Override
        public int sizeOf(int span, int days) {
            return span * days;
        }
    };

    private final int cycle;

    TimeWindowUnit(int cycle) {
        this.cycle = cycle;
    }

    /** The local start of the window containing {@code timestamp}. */
    public abstract LocalDateTime locate(Instant timestamp, ZoneId zone, int span);

    /** How many windows of {@code span} units cover {@code days} days. */
    public abstract int sizeOf(int span, int days);

    /**
     * Whether windows of this span tile the enclosing cycle evenly.
     *
     * <p>{@link #locate} rounds down within a minute, hour or day, so a span that does not
     * divide the cycle leaves a short window at the end of every cycle: 7-minute windows
     * would end with a 4-minute one at the top of each hour.
     */
    public boolean isValidSpan(int span) {
        return span > 0 && cycle % span == 0;
    }
}
