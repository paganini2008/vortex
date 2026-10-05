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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import com.chaconneai.openspreader.cache.CacheStats;
import com.github.vortex.tsdb.timeseries.NumberMetric;
import com.github.vortex.tsdb.timeseries.NumberMetrics;

/**
 * The three value types a series may hold.
 *
 * <p>The cache aggregates in {@code double}, whatever the type. The type decides how an
 * incoming value is validated and how the aggregate is presented: a {@code long} series reports
 * integers, a {@code decimal} series reports {@link BigDecimal}s. Integers are exact up to
 * 2<sup>53</sup>, which is why {@link #LONG} refuses anything beyond it rather than store it
 * rounded.
 *
 * @Description: DataType
 * @Author: Fred Feng
 * @Date: 03/10/2026
 * @Version 1.0.0
 */
public enum DataType {

    LONG {

        @Override
        public double parse(String raw) {
            long value;
            try {
                value = number(raw).longValueExact();
            } catch (ArithmeticException e) {
                throw new IllegalArgumentException("Not an integer: " + raw);
            }
            if (Math.abs(value) > MAX_EXACT_LONG) {
                throw new IllegalArgumentException(
                        "Integer out of exact range (±2^53): " + raw);
            }
            return value;
        }

        @Override
        public NumberMetric<Long> toMetric(CacheStats stats, long timestamp) {
            if (stats.count() == 0) {
                return NumberMetrics.nullLongMetric(timestamp);
            }
            return new NumberMetrics.LongMetric(Math.round(stats.max()), Math.round(stats.min()),
                    Math.round(stats.sum()), stats.count(), timestamp);
        }

        @Override
        public NumberMetric<Long> empty(long timestamp) {
            return NumberMetrics.nullLongMetric(timestamp);
        }
    },

    DOUBLE {

        @Override
        public double parse(String raw) {
            return finite(number(raw).doubleValue(), raw);
        }

        @Override
        public NumberMetric<Double> toMetric(CacheStats stats, long timestamp) {
            if (stats.count() == 0) {
                return NumberMetrics.nullDoubleMetric(timestamp);
            }
            return new NumberMetrics.DoubleMetric(stats.max(), stats.min(), stats.sum(),
                    stats.count(), timestamp);
        }

        @Override
        public NumberMetric<Double> empty(long timestamp) {
            return NumberMetrics.nullDoubleMetric(timestamp);
        }
    },

    DECIMAL {

        @Override
        public double parse(String raw) {
            return finite(number(raw).doubleValue(), raw);
        }

        @Override
        public NumberMetric<BigDecimal> toMetric(CacheStats stats, long timestamp) {
            if (stats.count() == 0) {
                return NumberMetrics.nullDecimalMetric(timestamp);
            }
            return new NumberMetrics.DecimalMetric(decimal(stats.max()), decimal(stats.min()),
                    decimal(stats.sum()), stats.count(), timestamp);
        }

        @Override
        public NumberMetric<BigDecimal> empty(long timestamp) {
            return NumberMetrics.nullDecimalMetric(timestamp);
        }
    };

    /** The largest magnitude a double holds with every integer below it exact. */
    static final long MAX_EXACT_LONG = 1L << 53;

    /**
     * Decimal sums accumulate in binary, so 0.1 + 0.2 comes back as 0.30000000000000004.
     * Rounding to this many places hides that noise without hiding real precision.
     */
    private static final int DECIMAL_SCALE = 8;

    /**
     * Validates and converts a raw value.
     *
     * @throws IllegalArgumentException when the value does not fit this type
     */
    public abstract double parse(String raw);

    /** Presents one cache aggregate as a metric of this type. */
    public abstract NumberMetric<? extends Number> toMetric(CacheStats stats, long timestamp);

    public abstract NumberMetric<? extends Number> empty(long timestamp);

    /** Presents one stored double in this type, as {@link #toMetric} presents an aggregate. */
    public Number present(double value) {
        return switch (this) {
            case LONG -> Math.round(value);
            case DOUBLE -> value;
            case DECIMAL -> decimal(value);
        };
    }

    public String code() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static DataType of(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Data type is required (long/double/decimal)");
        }
        try {
            return valueOf(code.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Unknown data type: " + code + " (expected long/double/decimal)");
        }
    }

    private static BigDecimal number(String raw) {
        try {
            return new BigDecimal(raw.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Not a number: " + raw);
        }
    }

    private static double finite(double value, String raw) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            throw new IllegalArgumentException("Not a finite number: " + raw);
        }
        return value;
    }

    private static BigDecimal decimal(double value) {
        return BigDecimal.valueOf(value).setScale(DECIMAL_SCALE, RoundingMode.HALF_UP)
                .stripTrailingZeros();
    }
}
