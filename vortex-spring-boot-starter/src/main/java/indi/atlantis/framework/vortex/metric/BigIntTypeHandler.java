package indi.atlantis.framework.vortex.metric;

import indi.atlantis.framework.vortex.common.Tuple;

/**
 * 
 * BigIntTypeHandler
 * 
 * @author Jimmy Hoff
 *
 * @version 1.0
 */
public class BigIntTypeHandler implements UserTypeHandler<String, BigInt> {

	@Override
	public String getDataTypeName() {
		return "bigint";
	}

	@Override
	public UserMetric<BigInt> convertAsMetric(String name, String metric, long timestamp, Tuple tuple) {
		long value = tuple.getField("value", Long.class);
		return new BigIntMetric(value, timestamp);
	}

	@Override
	public UserMetric<BigInt> convertAsMetric(String name, String metric, long timestamp, UserMetric<BigInt> metricUnit) {
		BigInt bigInt = metricUnit.get();
		long highestValue = bigInt.getHighestValue();
		long lowestValue = bigInt.getLowestValue();
		long totalValue = bigInt.getTotalValue();
		long count = bigInt.getCount();
		return new BigIntMetric(new BigInt(highestValue, lowestValue, totalValue, count), timestamp);
	}

	@Override
	public Tuple convertAsTuple(String topic, String name, String metric, long timestamp, UserMetric<BigInt> metricUnit) {
		BigInt bigInt = metricUnit.get();
		long highestValue = bigInt.getHighestValue();
		long lowestValue = bigInt.getLowestValue();
		long totalValue = bigInt.getTotalValue();
		long count = bigInt.getCount();
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
