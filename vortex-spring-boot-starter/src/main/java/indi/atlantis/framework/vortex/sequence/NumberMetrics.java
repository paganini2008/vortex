package indi.atlantis.framework.vortex.sequence;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

import com.github.paganini2008.devtools.primitives.Doubles;

import lombok.ToString;

/**
 * 
 * NumberMetrics
 *
 * @author Jimmy Hoff
 * @version 1.0
 */
public abstract class NumberMetrics {

	public static NumberMetric<Long> valueOf(long value, long timestamp) {
		return new LongMetric(value, timestamp);
	}

	public static NumberMetric<Double> valueOf(double value, long timestamp) {
		return new DoubleMetric(value, timestamp);
	}

	public static NumberMetric<BigDecimal> valueOf(BigDecimal value, long timestamp) {
		return new DecimalMetric(value, timestamp);
	}

	@ToString
	public static class DoubleMetric implements NumberMetric<Double> {

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
		public Double getMiddleValue() {
			return count > 0 ? Doubles.toFixed(totalValue / count, 4) : 0;
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
		public NumberMetric<Double> reset(NumberMetric<Double> currentMetric) {
			double highestValue = Double.max(this.highestValue, currentMetric.getHighestValue().doubleValue());
			double lowestValue = Double.min(this.lowestValue, currentMetric.getLowestValue().doubleValue());
			double totalValue = this.totalValue - currentMetric.getTotalValue().doubleValue();
			long count = this.count - currentMetric.getCount();
			long timestamp = currentMetric.getTimestamp();
			return new DoubleMetric(highestValue, lowestValue, totalValue, count, timestamp, false);
		}

		@Override
		public NumberMetric<Double> merge(NumberMetric<Double> anotherMetric) {
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
			data.put("middleValue", getMiddleValue());
			data.put("count", getCount());
			data.put("timestamp", getTimestamp());
			return data;
		}

	}

	@ToString
	public static class LongMetric implements NumberMetric<Long> {

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
		public Long getMiddleValue() {
			return count > 0 ? totalValue / count : 0;
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
		public NumberMetric<Long> reset(NumberMetric<Long> currentMetric) {
			long highestValue = Long.max(this.highestValue, currentMetric.getHighestValue().longValue());
			long lowestValue = Long.min(this.lowestValue, currentMetric.getLowestValue().longValue());
			long totalValue = this.totalValue - currentMetric.getTotalValue().longValue();
			long count = this.count - currentMetric.getCount();
			long timestamp = currentMetric.getTimestamp();
			return new LongMetric(highestValue, lowestValue, totalValue, count, timestamp, false);
		}

		@Override
		public NumberMetric<Long> merge(NumberMetric<Long> anotherMetric) {
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
			data.put("middleValue", getMiddleValue());
			data.put("count", getCount());
			data.put("timestamp", getTimestamp());
			return data;
		}

	}

	@ToString
	public static class DecimalMetric implements NumberMetric<BigDecimal> {

		DecimalMetric(BigDecimal value, long timestamp) {
			this.highestValue = value;
			this.lowestValue = value;
			this.totalValue = value;
			this.count = 1;
			this.timestamp = timestamp;
			this.reset = false;
		}

		public DecimalMetric(BigDecimal highestValue, BigDecimal lowestValue, BigDecimal totalValue, long count, long timestamp,
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
		public BigDecimal getMiddleValue() {
			return count > 0 ? totalValue.divide(BigDecimal.valueOf(count), 4, RoundingMode.HALF_UP) : BigDecimal.ZERO;
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
		public NumberMetric<BigDecimal> reset(NumberMetric<BigDecimal> currentMetric) {
			BigDecimal highestValue = this.highestValue.max((BigDecimal) currentMetric.getHighestValue());
			BigDecimal lowestValue = this.lowestValue.min((BigDecimal) currentMetric.getLowestValue());
			BigDecimal totalValue = this.totalValue.subtract((BigDecimal) currentMetric.getTotalValue());
			long count = this.count - currentMetric.getCount();
			long timestamp = currentMetric.getTimestamp();
			return new DecimalMetric(highestValue, lowestValue, totalValue, count, timestamp, false);
		}

		@Override
		public NumberMetric<BigDecimal> merge(NumberMetric<BigDecimal> anotherMetric) {
			BigDecimal highestValue = this.highestValue.max((BigDecimal) anotherMetric.getHighestValue());
			BigDecimal lowestValue = this.lowestValue.min((BigDecimal) anotherMetric.getLowestValue());
			BigDecimal totalValue = this.totalValue.add((BigDecimal) anotherMetric.getTotalValue());
			long count = this.count + anotherMetric.getCount();
			long timestamp = anotherMetric.getTimestamp();
			return new DecimalMetric(highestValue, lowestValue, totalValue, count, timestamp, false);
		}

		@Override
		public Map<String, Object> toEntries() {
			Map<String, Object> data = new HashMap<String, Object>();
			data.put("highestValue", getHighestValue());
			data.put("lowestValue", getLowestValue());
			data.put("middleValue", getMiddleValue());
			data.put("count", getCount());
			data.put("timestamp", getTimestamp());
			return data;
		}

	}

}
