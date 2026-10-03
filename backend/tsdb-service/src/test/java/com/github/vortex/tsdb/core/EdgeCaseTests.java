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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import com.chaconneai.openspreader.cache.CacheService;
import com.chaconneai.openspreader.cache.CacheStats;
import com.github.vortex.tsdb.timeseries.NumberMetric;
import com.github.vortex.tsdb.timeseries.NumberMetrics;
import com.github.vortex.tsdb.timeseries.TimeWindowUnit;

/**
 * The validation and fallback paths the integration tests do not reach.
 */
class EdgeCaseTests {

    private final TsdStoreProperties props = new TsdStoreProperties();

    @Test
    void timeWindowUnitsLocateAndSize() {
        Instant t = LocalDateTime.of(2026, 10, 3, 13, 47, 31).toInstant(ZoneOffset.UTC);
        assertThat(TimeWindowUnit.SECONDS.locate(t, ZoneOffset.UTC, 15))
                .isEqualTo(LocalDateTime.of(2026, 10, 3, 13, 47, 30));
        assertThat(TimeWindowUnit.HOURS.locate(t, ZoneOffset.UTC, 6))
                .isEqualTo(LocalDateTime.of(2026, 10, 3, 12, 0));
        assertThat(TimeWindowUnit.DAYS.locate(t, ZoneOffset.UTC, 1))
                .isEqualTo(LocalDateTime.of(2026, 10, 3, 0, 0));
        assertThat(TimeWindowUnit.SECONDS.sizeOf(15, 1)).isEqualTo(4 * 60 * 24);
        assertThat(TimeWindowUnit.SECONDS.sizeOf(7, 1)).isEqualTo(9 * 60 * 24);
        assertThat(TimeWindowUnit.MINUTES.sizeOf(5, 2)).isEqualTo(12 * 24 * 2);
        assertThat(TimeWindowUnit.MINUTES.sizeOf(7, 1)).isEqualTo(9 * 24);
        assertThat(TimeWindowUnit.HOURS.sizeOf(6, 1)).isEqualTo(4);
        assertThat(TimeWindowUnit.HOURS.sizeOf(5, 1)).isEqualTo(5);
        assertThat(TimeWindowUnit.DAYS.sizeOf(1, 7)).isEqualTo(7);
        assertThat(TimeWindowUnit.HOURS.isValidSpan(0)).isFalse();
    }

    @Test
    void queryWindowRejectsWhatItCannotAlign() {
        assertThatThrownBy(() -> QueryWindow.of("6h", 90, ZoneOffset.UTC, props))
                .hasMessageContaining("divide an hour");
        assertThatThrownBy(() -> QueryWindow.of("6h", 300, ZoneOffset.UTC, props))
                .hasMessageContaining("divide an hour");
        assertThatThrownBy(() -> QueryWindow.of("0h", 1, ZoneOffset.UTC, props))
                .hasMessageContaining("30m, 6h or 1d");
        assertThatThrownBy(() -> QueryWindow.of("1h", 1, 2000, ZoneOffset.UTC, props))
                .hasMessageContaining("retention");
        assertThatThrownBy(() -> QueryWindow.of("24h", 1, 1440, ZoneOffset.UTC, props))
                .hasMessageContaining("too wide");
        assertThat(QueryWindow.of("90m", 30, ZoneOffset.UTC, props).rangeText()).isEqualTo("90m");
        assertThat(QueryWindow.of(" ", null, ZoneOffset.UTC, props).rangeText()).isEqualTo("1h");
        assertThat(QueryWindow.defaultStep(Duration.ofDays(30), 1)).isEqualTo(720);
        assertThat(QueryWindow.defaultStep(Duration.ofHours(1), 7)).isEqualTo(720);
    }

    @Test
    void dataTypesCoverEveryShape() {
        assertThatThrownBy(() -> DataType.of(null)).hasMessageContaining("required");
        assertThatThrownBy(() -> DataType.of(" ")).hasMessageContaining("required");
        assertThatThrownBy(() -> DataType.LONG.parse("x")).hasMessageContaining("Not a number");
        assertThatThrownBy(() -> DataType.DECIMAL.parse("1e400")).hasMessageContaining("finite");
        assertThat(DataType.LONG.toMetric(CacheStats.EMPTY, 0).isEmpty()).isTrue();
        assertThat(DataType.DECIMAL.toMetric(CacheStats.EMPTY, 0).isEmpty()).isTrue();
        assertThat(DataType.LONG.empty(0).getCount()).isZero();
        assertThat(DataType.DOUBLE.empty(0).getCount()).isZero();
        assertThat(DataType.DECIMAL.empty(0).getCount()).isZero();
        assertThat(DataType.LONG.present(4.6)).isEqualTo(5L);
        assertThat(DataType.DOUBLE.present(4.6)).isEqualTo(4.6);
        assertThat((BigDecimal) DataType.DECIMAL.present(0.30000000000000004))
                .isEqualByComparingTo("0.3");
    }

    @Test
    void seriesKeysRefuseMalformedInput() {
        assertThatThrownBy(() -> new SeriesKey(null, "a", "b")).hasMessageContaining("required");
        assertThatThrownBy(() -> SeriesKey.of("long", null, "b"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SeriesKey.fromCatalogMember("long|a".getBytes()))
                .hasMessageContaining("Malformed");
        assertThatThrownBy(() -> SeriesKey.fromLastValueField("a", "nodelimiter"))
                .hasMessageContaining("Malformed");
        assertThat(SeriesKey.fromLastValueField("car", "double|fuel"))
                .isEqualTo(SeriesKey.of("double", "car", "fuel"));
        assertThat(SeriesKey.checkCategory(" car ")).isEqualTo("car");
    }

    @Test
    void metricsMergeAroundEmptiesAndNulls() {
        NumberMetric<BigDecimal> one = NumberMetrics.valueOf(BigDecimal.ONE, 1);
        NumberMetric<BigDecimal> empty = NumberMetrics.nullDecimalMetric(0);
        assertThat(one.merge(null)).isSameAs(one);
        assertThat(one.merge(empty)).isSameAs(one);
        assertThat(empty.merge(one)).isSameAs(one);
        assertThat(empty.getAverageValue()).isNull();
        assertThat(NumberMetrics.valueOf(1L, 0).merge(null).getCount()).isEqualTo(1);
        assertThat(NumberMetrics.valueOf(1.0, 0).merge(null).getCount()).isEqualTo(1);
        assertThat(NumberMetrics.nullLongMetric(0).getAverageValue()).isNull();
        assertThat(NumberMetrics.nullDoubleMetric(0).merge(NumberMetrics.valueOf(2.0, 0)).getCount())
                .isEqualTo(1);
        assertThat(NumberMetrics.nullLongMetric(0).merge(NumberMetrics.valueOf(2L, 0)).getCount())
                .isEqualTo(1);
        assertThat(NumberMetrics.valueOf(2.0, 0).merge(NumberMetrics.nullDoubleMetric(0))
                .getCount()).isEqualTo(1);
        assertThat(NumberMetrics.valueOf(2L, 0).merge(NumberMetrics.nullLongMetric(0)).getCount())
                .isEqualTo(1);
    }

    @Test
    void lastValuesDecodeOnlyWhatWasEncoded() {
        assertThat(LastValue.decode(DataType.LONG, null)).isNull();
        assertThat(LastValue.decode(DataType.LONG, new byte[3])).isNull();
        assertThat(LastValue.decode(DataType.LONG, LastValue.encode(7, 99)))
                .isEqualTo(new LastValue(7L, 99));
    }

    @Test
    void snapshotTaskSchedulesOnlyWhenItCanWrite() {
        CacheService service = mock(CacheService.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<CacheService> provider = mock(ObjectProvider.class);
        ScheduledTaskRegistrar registrar = mock(ScheduledTaskRegistrar.class);

        when(provider.getIfAvailable()).thenReturn(null);
        new CacheSnapshotTask(provider, props).configureTasks(registrar);
        when(provider.getIfAvailable()).thenReturn(service);
        props.setSnapshotInterval(Duration.ZERO);
        new CacheSnapshotTask(provider, props).configureTasks(registrar);
        props.setSnapshotInterval(Duration.ofSeconds(-1));
        new CacheSnapshotTask(provider, props).configureTasks(registrar);
        props.setSnapshotInterval(null);
        new CacheSnapshotTask(provider, props).configureTasks(registrar);
        verify(registrar, never()).addFixedDelayTask(any(Runnable.class), any(Duration.class));

        props.setSnapshotInterval(Duration.ofMinutes(5));
        new CacheSnapshotTask(provider, props).configureTasks(registrar);
        ArgumentCaptor<Runnable> task = ArgumentCaptor.forClass(Runnable.class);
        verify(registrar).addFixedDelayTask(task.capture(), eq(Duration.ofMinutes(5)));
        when(service.dumpToDisk()).thenReturn(2048L, -1L);
        task.getValue().run();
        task.getValue().run();
        verify(service, org.mockito.Mockito.times(2)).dumpToDisk();
    }

    @Test
    void pruneTaskDelegates() {
        TsdStoreService service = mock(TsdStoreService.class);
        when(service.pruneCatalog()).thenReturn(2, 0);
        CatalogPruneTask task = new CatalogPruneTask(service);
        task.prune();
        task.prune();
        verify(service, org.mockito.Mockito.times(2)).pruneCatalog();
    }

    @Test
    void spanMustDivideAnHour() {
        props.setSpan(7);
        assertThatThrownBy(() -> new TsdStoreService(null, props))
                .hasMessageContaining("must divide 60");
    }

    @Test
    void externalStoreIsRedisUnderTheConfiguredPrefix() {
        com.chaconneai.openspreader.MultiProcessingProperties props =
                new com.chaconneai.openspreader.MultiProcessingProperties();
        props.getCache().getExternal().setKeyPrefix("vortex:cache:");
        assertThat(new ExternalCacheStoreConfig().externalCacheStore(
                mock(org.springframework.data.redis.connection.RedisConnectionFactory.class), props))
                .isInstanceOf(com.chaconneai.openspreader.cache.RedisCacheStore.class);
    }
}
