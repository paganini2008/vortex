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
public class NumericTypeHandler implements UserTypeHandler<String, Numeric> {

	@Override
	public String getDataTypeName() {
		return "numeric";
	}

	@Override
	public UserMetric<Numeric> convertAsMetric(String name, String metric, long timestamp, Tuple tuple) {
		BigDecimal value = tuple.getField("value", BigDecimal.class);
		return new NumericMetric(value, timestamp);
	}

	@Override
	public UserMetric<Numeric> convertAsMetric(String name, String metric, long timestamp, UserMetric<Numeric> metricUnit) {
		Numeric numeric = metricUnit.get();
		BigDecimal highestValue = numeric.getHighestValue();
		BigDecimal lowestValue = numeric.getLowestValue();
		BigDecimal totalValue = numeric.getTotalValue();
		long count = numeric.getCount();
		return new NumericMetric(new Numeric(highestValue, lowestValue, totalValue, count), timestamp);
	}

	@Override
	public Tuple convertAsTuple(String topic, String name, String metric, long timestamp, UserMetric<Numeric> metricUnit) {
		Numeric numeric = metricUnit.get();
		BigDecimal highestValue = numeric.getHighestValue();
		BigDecimal lowestValue = numeric.getLowestValue();
		BigDecimal totalValue = numeric.getTotalValue();
		long count = numeric.getCount();

		Tuple tuple = Tuple.newOne(topic);
		tuple.setField("name", name);
		tuple.setField("metric", metric);
		tuple.setField("highestValue", highestValue);
		tuple.setField("lowestValue", lowestValue);
		tuple.setField("totalValue", totalValue);
		tuple.setField("count", count);
		tuple.setField("timestamp", timestamp);
		return tuple;
	}

}
