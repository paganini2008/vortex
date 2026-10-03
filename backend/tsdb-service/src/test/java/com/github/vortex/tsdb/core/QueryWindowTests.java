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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

class QueryWindowTests {

    private final TsdStoreProperties props = new TsdStoreProperties();

    @Test
    void defaultsToAboutSixtyWindows() {
        assertThat(QueryWindow.of(null, null, ZoneOffset.UTC, props).stepMinutes()).isEqualTo(1);
        assertThat(QueryWindow.of("6h", null, ZoneOffset.UTC, props).stepMinutes()).isEqualTo(10);
        assertThat(QueryWindow.of("1d", null, ZoneOffset.UTC, props).stepMinutes()).isEqualTo(30);
        assertThat(QueryWindow.of("24h", 60, ZoneOffset.UTC, props).rangeText()).isEqualTo("1d");
    }

    @Test
    void refusesStepsThatDoNotAlignAndRangesBeyondRetention() {
        for (Integer step : new Integer[] {7, 45, 90, 0, -5}) {
            assertThatThrownBy(() -> QueryWindow.of("6h", step, ZoneOffset.UTC, props))
                    .isInstanceOf(IllegalArgumentException.class);
        }
        assertThatThrownBy(() -> QueryWindow.of("2d", 60, ZoneOffset.UTC, props))
                .hasMessageContaining("retention");
        assertThatThrownBy(() -> QueryWindow.of("50m", 15, ZoneOffset.UTC, props))
                .hasMessageContaining("whole number");
        assertThatThrownBy(() -> QueryWindow.of("6 hours", 5, ZoneOffset.UTC, props))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void hourlyWindowsStartOnTheLocalHour() {
        ZoneId kolkata = ZoneId.of("Asia/Kolkata"); // +05:30
        long now = LocalDateTime.of(2026, 10, 3, 9, 47).atZone(kolkata).toInstant().toEpochMilli();
        List<QueryWindow.Window> w = QueryWindow.of("3h", 60, kolkata, props).windows(now);
        assertThat(w).hasSize(3);
        assertThat(w.get(2).from())
                .isEqualTo(LocalDateTime.of(2026, 10, 3, 9, 0).atZone(kolkata).toInstant().toEpochMilli());
        assertThat(w.get(0).to()).isEqualTo(w.get(1).from());
        assertThat(w.get(2).to() - w.get(2).from()).isEqualTo(3_600_000);
    }

    @Test
    void windowsFollowTheOffsetOfTheirOwnDate() {
        // Sydney moves its clocks forward at 02:00 on 2026-10-04
        ZoneId sydney = ZoneId.of("Australia/Sydney");
        long now = LocalDateTime.of(2026, 10, 4, 5, 10).atZone(sydney).toInstant().toEpochMilli();
        List<QueryWindow.Window> w = QueryWindow.of("6h", 60, sydney, props).windows(now);
        // 05:00 is DST, 23:00 the day before is not: every window starts on its local hour
        assertThat(w.get(5).from()).isEqualTo(
                LocalDateTime.of(2026, 10, 4, 5, 0).atZone(sydney).toInstant().toEpochMilli());
        assertThat(w.get(0).from()).isEqualTo(
                LocalDateTime.of(2026, 10, 3, 23, 0).atZone(sydney).toInstant().toEpochMilli());
        // No duplicate or stretched window across the change
        for (int i = 1; i < w.size(); i++) {
            assertThat(w.get(i).from() - w.get(i - 1).from()).isEqualTo(3_600_000);
        }
    }

    @Test
    void slidingWindowsEndWithTheirStep() {
        long now = LocalDateTime.of(2026, 10, 3, 9, 47, 30).toInstant(ZoneOffset.UTC).toEpochMilli();
        QueryWindow q = QueryWindow.of("10m", 1, 5, ZoneOffset.UTC, props);
        assertThat(q.isSliding()).isTrue();
        QueryWindow.Window last = q.windows(now).get(9);
        long slot = LocalDateTime.of(2026, 10, 3, 9, 47).toInstant(ZoneOffset.UTC).toEpochMilli();
        assertThat(last.slot()).isEqualTo(slot);
        assertThat(last.to()).isEqualTo(slot + 60_000);
        assertThat(last.from()).isEqualTo(slot + 60_000 - 300_000);
        assertThat(QueryWindow.of("10m", 1, null, ZoneOffset.UTC, props).isSliding()).isFalse();
    }
}
