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
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import com.chaconneai.openspreader.cache.CacheStats;
import com.github.vortex.tsdb.timeseries.NumberMetric;

class SeriesKeyAndDataTypeTests {

    @Test
    void keysAndCatalogMembersRoundTrip() {
        SeriesKey key = SeriesKey.of("LONG", "car", "speed");
        assertThat(key.bucketKey(60_000)).isEqualTo("tsd:long:car:speed:60000");
        assertThat(key.keyPattern()).isEqualTo("tsd:long:car:speed:*");
        assertThat(SeriesKey.fromCatalogMember(key.catalogMember())).isEqualTo(key);
        assertThat(SeriesKey.of("double", "传感器", "温度").category()).isEqualTo("传感器");
    }

    @Test
    void namesThatWouldBreakTheKeyLayoutAreRefused() {
        for (String bad : new String[] {"a:b", "a|b", "a*", "a?", "", " ", "a b"}) {
            assertThatThrownBy(() -> SeriesKey.of("long", bad, "speed"))
                    .isInstanceOf(IllegalArgumentException.class);
        }
        assertThatThrownBy(() -> SeriesKey.of("float", "car", "speed"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void longRefusesFractionsAndInexactMagnitudes() {
        assertThat(DataType.LONG.parse("44")).isEqualTo(44d);
        assertThat(DataType.LONG.parse("44.0")).isEqualTo(44d);
        assertThatThrownBy(() -> DataType.LONG.parse("44.5"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> DataType.LONG.parse(String.valueOf((1L << 53) + 1)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> DataType.DOUBLE.parse("NaN"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> DataType.DECIMAL.parse("abc"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void statsArePresentedInTheSeriesType() {
        CacheStats stats = new CacheStats(9, 1, 0.30000000000000004, 3);
        NumberMetric<?> decimal = DataType.DECIMAL.toMetric(stats, 0);
        assertThat((BigDecimal) decimal.getTotalValue()).isEqualByComparingTo("0.3");
        assertThat(DataType.LONG.toMetric(new CacheStats(9, 1, 15, 3), 0).getHighestValue())
                .isEqualTo(9L);
        assertThat(DataType.DOUBLE.toMetric(CacheStats.EMPTY, 0).getCount()).isZero();
        assertThat(DataType.DOUBLE.toMetric(CacheStats.EMPTY, 0).getHighestValue()).isNull();
    }
}
