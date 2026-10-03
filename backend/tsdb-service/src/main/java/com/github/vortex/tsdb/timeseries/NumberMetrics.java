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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.ToString;

/**
 * Number metrics for the three supported data types.
 *
 * <p>Carried over from doodler-common-timeseries. Differences from the original:
 * <ul>
 * <li>{@code baseValue}/{@code baseCount} are gone: nothing ever supplied a baseline, so they
 * always equalled the value and the count</li>
 * <li>merging an empty metric no longer throws: an empty side is simply the identity</li>
 * <li>{@code LongMetric} averaged with integer division; it now divides as a double</li>
 * <li>the average is rounded with {@link BigDecimal} rather than a {@code DecimalFormat},
 * which printed a comma under some locales and then failed to parse back</li>
 * </ul>
 *
 * @Description: NumberMetrics
 * @Author: Fred Feng
 * @Date: 15/11/2024
 * @Version 1.0.0
 */
public abstract class NumberMetrics {

    private static final int AVERAGE_SCALE = 4;

    public static LongMetric nullLongMetric(long timestamp) {
        return new LongMetric(null, null, null, 0, timestamp);
    }

    public static LongMetric valueOf(long value, long timestamp) {
        return new LongMetric(value, value, value, 1, timestamp);
    }

    public static DoubleMetric nullDoubleMetric(long timestamp) {
        return new DoubleMetric(null, null, null, 0, timestamp);
    }

    public static DoubleMetric valueOf(double value, long timestamp) {
        return new DoubleMetric(value, value, value, 1, timestamp);
    }

    public static DecimalMetric nullDecimalMetric(long timestamp) {
        return new DecimalMetric(null, null, null, 0, timestamp);
    }

    public static DecimalMetric valueOf(BigDecimal value, long timestamp) {
        return new DecimalMetric(value, value, value, 1, timestamp);
    }

    static Double average(double total, long count) {
        if (count <= 0) {
            return null;
        }
        return BigDecimal.valueOf(total / count).setScale(AVERAGE_SCALE, RoundingMode.HALF_UP)
                .doubleValue();
    }

    static Map<String, Object> represent(NumberMetric<?> metric) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("count", metric.getCount());
        data.put("highestValue", metric.getHighestValue());
        data.put("lowestValue", metric.getLowestValue());
        data.put("totalValue", metric.getTotalValue());
        data.put("averageValue", metric.getAverageValue());
        data.put("timestamp", metric.getTimestamp());
        return data;
    }

    /**
     *
     * @Description: DoubleMetric
     * @Author: Fred Feng
     * @Date: 15/11/2024
     * @Version 1.0.0
     */
    @ToString
    public static class DoubleMetric implements NumberMetric<Double> {

        private final Double highestValue;
        private final Double lowestValue;
        private final Double totalValue;
        private final long count;
        private final long timestamp;

        public DoubleMetric(Double highestValue, Double lowestValue, Double totalValue, long count,
                long timestamp) {
            this.highestValue = highestValue;
            this.lowestValue = lowestValue;
            this.totalValue = totalValue;
            this.count = count;
            this.timestamp = timestamp;
        }

        @Override
        public Double getHighestValue() {
            return highestValue;
        }

        @Override
        public Double getLowestValue() {
            return lowestValue;
        }

        @Override
        public Double getTotalValue() {
            return totalValue;
        }

        @Override
        public long getCount() {
            return count;
        }

        @Override
        public Double getAverageValue() {
            return count > 0 ? average(totalValue, count) : null;
        }

        @Override
        public long getTimestamp() {
            return timestamp;
        }

        @Override
        public NumberMetric<Double> merge(NumberMetric<Double> other) {
            if (other == null || other.isEmpty()) {
                return this;
            }
            if (isEmpty()) {
                return other;
            }
            return new DoubleMetric(Math.max(highestValue, other.getHighestValue()),
                    Math.min(lowestValue, other.getLowestValue()),
                    totalValue + other.getTotalValue(), count + other.getCount(),
                    Math.max(timestamp, other.getTimestamp()));
        }

        @Override
        public Map<String, Object> represent() {
            return NumberMetrics.represent(this);
        }
    }

    /**
     *
     * @Description: LongMetric
     * @Author: Fred Feng
     * @Date: 15/11/2024
     * @Version 1.0.0
     */
    @ToString
    public static class LongMetric implements NumberMetric<Long> {

        private final Long highestValue;
        private final Long lowestValue;
        private final Long totalValue;
        private final long count;
        private final long timestamp;

        public LongMetric(Long highestValue, Long lowestValue, Long totalValue, long count,
                long timestamp) {
            this.highestValue = highestValue;
            this.lowestValue = lowestValue;
            this.totalValue = totalValue;
            this.count = count;
            this.timestamp = timestamp;
        }

        @Override
        public Long getHighestValue() {
            return highestValue;
        }

        @Override
        public Long getLowestValue() {
            return lowestValue;
        }

        @Override
        public Long getTotalValue() {
            return totalValue;
        }

        @Override
        public long getCount() {
            return count;
        }

        @Override
        public Double getAverageValue() {
            return count > 0 ? average(totalValue.doubleValue(), count) : null;
        }

        @Override
        public long getTimestamp() {
            return timestamp;
        }

        @Override
        public NumberMetric<Long> merge(NumberMetric<Long> other) {
            if (other == null || other.isEmpty()) {
                return this;
            }
            if (isEmpty()) {
                return other;
            }
            return new LongMetric(Math.max(highestValue, other.getHighestValue()),
                    Math.min(lowestValue, other.getLowestValue()),
                    totalValue + other.getTotalValue(), count + other.getCount(),
                    Math.max(timestamp, other.getTimestamp()));
        }

        @Override
        public Map<String, Object> represent() {
            return NumberMetrics.represent(this);
        }
    }

    /**
     *
     * @Description: DecimalMetric
     * @Author: Fred Feng
     * @Date: 15/11/2024
     * @Version 1.0.0
     */
    @ToString
    public static class DecimalMetric implements NumberMetric<BigDecimal> {

        private final BigDecimal highestValue;
        private final BigDecimal lowestValue;
        private final BigDecimal totalValue;
        private final long count;
        private final long timestamp;

        public DecimalMetric(BigDecimal highestValue, BigDecimal lowestValue,
                BigDecimal totalValue, long count, long timestamp) {
            this.highestValue = highestValue;
            this.lowestValue = lowestValue;
            this.totalValue = totalValue;
            this.count = count;
            this.timestamp = timestamp;
        }

        @Override
        public BigDecimal getHighestValue() {
            return highestValue;
        }

        @Override
        public BigDecimal getLowestValue() {
            return lowestValue;
        }

        @Override
        public BigDecimal getTotalValue() {
            return totalValue;
        }

        @Override
        public long getCount() {
            return count;
        }

        @Override
        public Double getAverageValue() {
            return count > 0
                    ? totalValue.divide(BigDecimal.valueOf(count), AVERAGE_SCALE,
                            RoundingMode.HALF_UP).doubleValue()
                    : null;
        }

        @Override
        public long getTimestamp() {
            return timestamp;
        }

        @Override
        public NumberMetric<BigDecimal> merge(NumberMetric<BigDecimal> other) {
            if (other == null || other.isEmpty()) {
                return this;
            }
            if (isEmpty()) {
                return other;
            }
            return new DecimalMetric(highestValue.max(other.getHighestValue()),
                    lowestValue.min(other.getLowestValue()),
                    totalValue.add(other.getTotalValue()), count + other.getCount(),
                    Math.max(timestamp, other.getTimestamp()));
        }

        @Override
        public Map<String, Object> represent() {
            return NumberMetrics.represent(this);
        }
    }
}
