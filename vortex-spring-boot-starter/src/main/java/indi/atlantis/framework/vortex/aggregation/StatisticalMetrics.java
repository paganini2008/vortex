package indi.atlantis.framework.vortex.aggregation;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

import com.github.paganini2008.devtools.beans.ToStringBuilder;
import com.github.paganini2008.devtools.primitives.Doubles;

/**
 * 
 * StatisticalMetrics
 *
 * @author Jimmy Hoff
 * @version 1.0
 */
public abstract class StatisticalMetrics {

	public static StatisticalMetric valueOf(long value, long timestamp) {
		return new LongMetric(value, timestamp);
	}

	public static StatisticalMetric valueOf(double value, long timestamp) {
		return new DoubleMetric(value, timestamp);
	}

	public static StatisticalMetric valueOf(BigDecimal value, long timestamp) {
		return new BigDecimalMetric(value, timestamp);
	}

	public static class DoubleMetric implements StatisticalMetric {

		DoubleMetric(double value, long timestamp) {
			this.highestValue = value;
			this.lowestValue = value;
			this.totalValue = value;
			this.count = 1;
			this.timestamp = timestamp;
			this.reset = false;
		}

		public DoubleMetric(double highestValue, double lowestValue, double totalValue, long count, long timestamp, boolean reset) {
			this.highestValue = highestValue;
			this.lowestValue = lowestValue;
			this.totalValue = totalValue;
			this.count = count;
			this.timestamp = timestamp;
			this.reset = reset;
		}

		private double highestValue;
		private double lowestValue;
		private double totalValue;
		private long count;
		private long timestamp;
		private final boolean reset;

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
		public Double getMiddleValue(int scale) {
			return count > 0 ? scale > 0 ? Doubles.toFixed(totalValue / count, scale) : totalValue / count : 0;
		}

		@Override
		public long getTimestamp() {
			return timestamp;
		}

		@Override
		public boolean reset() {
			return reset;
		}

		@Override
		public StatisticalMetric reset(StatisticalMetric currentMetric) {
			double totalValue = this.totalValue - currentMetric.getTotalValue().doubleValue();
			long count = this.count - currentMetric.getCount();
			return new DoubleMetric(highestValue, lowestValue, totalValue, count, timestamp, false);
		}

		@Override
		public StatisticalMetric merge(StatisticalMetric anotherMetric) {
			double highestValue = Double.max(this.highestValue, anotherMetric.getHighestValue().doubleValue());
			double lowestValue = Double.min(this.lowestValue, anotherMetric.getLowestValue().doubleValue());
			double totalValue = this.totalValue + anotherMetric.getTotalValue().doubleValue();
			long count = this.count + anotherMetric.getCount();
			long timestamp = anotherMetric.getTimestamp();
			return new DoubleMetric(highestValue, lowestValue, totalValue, count, timestamp, false);
		}

		@Override
		public Map<String, Object> toEntries() {
			Map<String, Object> data = new HashMap<String, Object>();
			data.put("highestValue", getHighestValue());
			data.put("lowestValue", getLowestValue());
			data.put("middleValue", getMiddleValue(1));
			data.put("count", getCount());
			data.put("timestamp", getTimestamp());
			return data;
		}

		public String toString() {
			return ToStringBuilder.reflectionToString(this);
		}

	}

	public static class LongMetric implements StatisticalMetric {

		LongMetric(long value, long timestamp) {
			this.highestValue = value;
			this.lowestValue = value;
			this.totalValue = value;
			this.count = 1;
			this.timestamp = timestamp;
			this.reset = false;
		}

		public LongMetric(long highestValue, long lowestValue, long totalValue, long count, long timestamp, boolean reset) {
			this.highestValue = highestValue;
			this.lowestValue = lowestValue;
			this.totalValue = totalValue;
			this.count = count;
			this.timestamp = timestamp;
			this.reset = reset;
		}

		private long highestValue;
		private long lowestValue;
		private long totalValue;
		private long count;
		private long timestamp;
		private final boolean reset;

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
		public Double getMiddleValue(int scale) {
			return count > 0 ? scale > 0 ? Doubles.toFixed((double) totalValue / count, scale) : totalValue / count : 0;
		}

		@Override
		public long getTimestamp() {
			return timestamp;
		}

		@Override
		public boolean reset() {
			return reset;
		}

		@Override
		public StatisticalMetric reset(StatisticalMetric currentMetric) {
			long totalValue = this.totalValue - currentMetric.getTotalValue().longValue();
			long count = this.count - currentMetric.getCount();
			return new LongMetric(highestValue, lowestValue, totalValue, count, timestamp, false);
		}

		@Override
		public StatisticalMetric merge(StatisticalMetric anotherMetric) {
			long highestValue = Long.max(this.highestValue, anotherMetric.getHighestValue().longValue());
			long lowestValue = Long.min(this.lowestValue, anotherMetric.getLowestValue().longValue());
			long totalValue = this.totalValue + anotherMetric.getTotalValue().longValue();
			long count = this.count + anotherMetric.getCount();
			long timestamp = anotherMetric.getTimestamp();
			return new LongMetric(highestValue, lowestValue, totalValue, count, timestamp, false);
		}

		@Override
		public Map<String, Object> toEntries() {
			Map<String, Object> data = new HashMap<String, Object>();
			data.put("highestValue", getHighestValue());
			data.put("lowestValue", getLowestValue());
			data.put("middleValue", getMiddleValue(0));
			data.put("count", getCount());
			data.put("timestamp", getTimestamp());
			return data;
		}

		public String toString() {
			return ToStringBuilder.reflectionToString(this);
		}

	}

	public static class BigDecimalMetric implements StatisticalMetric {

		BigDecimalMetric(BigDecimal value, long timestamp) {
			this.highestValue = value;
			this.lowestValue = value;
			this.totalValue = value;
			this.count = 1;
			this.timestamp = timestamp;
			this.reset = false;
		}

		public BigDecimalMetric(BigDecimal highestValue, BigDecimal lowestValue, BigDecimal totalValue, long count, long timestamp,
				boolean reset) {
			this.highestValue = highestValue;
			this.lowestValue = lowestValue;
			this.totalValue = totalValue;
			this.count = count;
			this.timestamp = timestamp;
			this.reset = reset;
		}

		private BigDecimal highestValue;
		private BigDecimal lowestValue;
		private BigDecimal totalValue;
		private long count;
		private long timestamp;
		private final boolean reset;

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
		public BigDecimal getMiddleValue(int scale) {
			return count > 0
					? scale > 0 ? totalValue.divide(BigDecimal.valueOf(count), scale, RoundingMode.HALF_UP)
							: totalValue.divide(BigDecimal.valueOf(count))
					: BigDecimal.ZERO;
		}

		@Override
		public long getTimestamp() {
			return timestamp;
		}

		@Override
		public boolean reset() {
			return reset;
		}

		@Override
		public StatisticalMetric reset(StatisticalMetric currentMetric) {
			BigDecimal totalValue = this.totalValue.subtract((BigDecimal) currentMetric.getTotalValue());
			long count = this.count - currentMetric.getCount();
			return new BigDecimalMetric(highestValue, lowestValue, totalValue, count, timestamp, false);
		}

		@Override
		public StatisticalMetric merge(StatisticalMetric anotherMetric) {
			BigDecimal highestValue = this.highestValue.max((BigDecimal) anotherMetric.getHighestValue());
			BigDecimal lowestValue = this.lowestValue.min((BigDecimal) anotherMetric.getLowestValue());
			BigDecimal totalValue = this.totalValue.add((BigDecimal) anotherMetric.getTotalValue());
			long count = this.count + anotherMetric.getCount();
			long timestamp = anotherMetric.getTimestamp();
			return new BigDecimalMetric(highestValue, lowestValue, totalValue, count, timestamp, false);
		}

		@Override
		public Map<String, Object> toEntries() {
			Map<String, Object> data = new HashMap<String, Object>();
			data.put("highestValue", getHighestValue());
			data.put("lowestValue", getLowestValue());
			data.put("middleValue", getMiddleValue(1));
			data.put("count", getCount());
			data.put("timestamp", getTimestamp());
			return data;
		}

		public String toString() {
			return ToStringBuilder.reflectionToString(this);
		}

	}

}
