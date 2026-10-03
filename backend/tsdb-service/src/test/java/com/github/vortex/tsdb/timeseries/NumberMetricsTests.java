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

import static org.assertj.core.api.Assertions.assertThat;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.Test;

class NumberMetricsTests {

    @Test
    void longAverageIsNotIntegerDivision() {
        NumberMetric<Long> m = NumberMetrics.valueOf(1L, 0).merge(NumberMetrics.valueOf(2L, 0));
        assertThat(m.getAverageValue()).isEqualTo(1.5);
        assertThat(m.getCount()).isEqualTo(2);
        assertThat(m.getHighestValue()).isEqualTo(2L);
        assertThat(m.getLowestValue()).isEqualTo(1L);
        assertThat(m.getTotalValue()).isEqualTo(3L);
    }

    @Test
    void emptyIsTheIdentityOfMerge() {
        NumberMetric<Double> empty = NumberMetrics.nullDoubleMetric(0);
        NumberMetric<Double> one = NumberMetrics.valueOf(4.5, 10);
        assertThat(empty.merge(one)).isSameAs(one);
        assertThat(one.merge(empty)).isSameAs(one);
        assertThat(empty.getAverageValue()).isNull();
        assertThat(empty.getHighestValue()).isNull();
    }

    @Test
    void decimalMergeKeepsPrecision() {
        NumberMetric<BigDecimal> m = NumberMetrics.valueOf(new BigDecimal("0.1"), 0)
                .merge(NumberMetrics.valueOf(new BigDecimal("0.2"), 0));
        assertThat(m.getTotalValue()).isEqualByComparingTo("0.3");
        assertThat(m.getAverageValue()).isEqualTo(0.15);
    }

    @Test
    void averageDoesNotDependOnLocale() {
        Locale saved = Locale.getDefault();
        try {
            Locale.setDefault(Locale.GERMANY);
            assertThat(NumberMetrics.valueOf(1.0, 0).merge(NumberMetrics.valueOf(2.0, 0))
                    .getAverageValue()).isEqualTo(1.5);
        } finally {
            Locale.setDefault(saved);
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void representKeepsTheFieldsTheOldUiReads() {
        Map<String, Object> data = (Map<String, Object>) NumberMetrics.valueOf(7L, 5).represent();
        assertThat(data).containsKeys("count", "highestValue", "lowestValue", "averageValue",
                "totalValue", "timestamp");
    }

    @Test
    void minutesLocateRoundsDownToTheSpan() {
        Instant t = LocalDateTime.of(2026, 10, 3, 9, 47, 31).toInstant(ZoneOffset.UTC);
        assertThat(TimeWindowUnit.MINUTES.locate(t, ZoneOffset.UTC, 5))
                .isEqualTo(LocalDateTime.of(2026, 10, 3, 9, 45, 0));
        assertThat(TimeWindowUnit.MINUTES.locate(t, ZoneId.of("Australia/Sydney"), 1))
                .isEqualTo(LocalDateTime.of(2026, 10, 3, 19, 47, 0));
        assertThat(TimeWindowUnit.MINUTES.isValidSpan(15)).isTrue();
        assertThat(TimeWindowUnit.MINUTES.isValidSpan(7)).isFalse();
    }
}
