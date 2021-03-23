package indi.atlantis.framework.vortex.metric;

import java.math.BigDecimal;

import indi.atlantis.framework.vortex.common.Tuple;

/**
 * 
 * NumericTypeHandler
 * 
 * @author Jimmy Hoff
 *
 * @version 1.0
 */
public class NumericTypeHandler implements UserTypeHandler<Numeric> {

	@Override
	public String getDataTypeName() {
		return "numeric";
	}

	@Override
	public UserMetric<Numeric> convertAsMetric(String identifier, String metric, long timestamp, Tuple tuple) {
		if (tuple.hasField("value")) {
			BigDecimal value = tuple.getField("value", BigDecimal.class);
			return new NumericMetric(value, timestamp);
		} else {
			BigDecimal highestValue = tuple.getField("highestValue", BigDecimal.class);
			BigDecimal lowestValue = tuple.getField("lowestValue", BigDecimal.class);
			BigDecimal totalValue = tuple.getField("totalValue", BigDecimal.class);
			long count = tuple.getField("count", Long.class);
			return new NumericMetric(new Numeric(highestValue, lowestValue, totalValue, count), timestamp);
		}
	}

	@Override
	public UserMetric<Numeric> convertAsMetric(String identifier, String metric, long timestamp, UserMetric<Numeric> metricUnit) {
		Numeric numeric = metricUnit.get();
		BigDecimal highestValue = numeric.getHighestValue();
		BigDecimal lowestValue = numeric.getLowestValue();
		BigDecimal totalValue = numeric.getTotalValue();
		long count = numeric.getCount();
		return new NumericMetric(new Numeric(highestValue, lowestValue, totalValue, count), timestamp);
	}

	@Override
	public Tuple convertAsTuple(String topic, String identifier, String metric, long timestamp, UserMetric<Numeric> metricUnit) {
		Numeric numeric = metricUnit.get();
		BigDecimal highestValue = numeric.getHighestValue();
		BigDecimal lowestValue = numeric.getLowestValue();
		BigDecimal totalValue = numeric.getTotalValue();
		long count = numeric.getCount();

		Tuple tuple = Tuple.newOne(topic);
		tuple.setField("name", identifier);
		tuple.setField("metric", metric);
		tuple.setField("highestValue", highestValue);
		tuple.setField("lowestValue", lowestValue);
		tuple.setField("totalValue", totalValue);
		tuple.setField("count", count);
		tuple.setField("timestamp", timestamp);
		return tuple;
	}

}
