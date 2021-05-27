package indi.atlantis.framework.vortex.metric;

import indi.atlantis.framework.vortex.common.Tuple;

/**
 * 
 * BoolTypeHandler
 * 
 * @author Fred Feng
 *
 * @version 1.0
 */
public class BoolTypeHandler implements UserTypeHandler<Bool> {

	@Override
	public String getDataTypeName() {
		return "bool";
	}

	@Override
	public UserMetric<Bool> convertAsMetric(String identifier, String metric, long timestamp, Tuple tuple) {
		boolean value = tuple.getField("value", Boolean.class);
		return new BoolMetric(value, timestamp);
	}

	@Override
	public UserMetric<Bool> convertAsMetric(String identifier, String metric, long timestamp, UserMetric<Bool> metricUnit) {
		long yes = metricUnit.get().getYes();
		long no = metricUnit.get().getNo();
		return new BoolMetric(new Bool(yes, no), timestamp);
	}

	@Override
	public Tuple convertAsTuple(String topic, String identifier, String metric, long timestamp, UserMetric<Bool> metricUnit) {
		long yes = metricUnit.get().getYes();
		long no = metricUnit.get().getNo();
		Tuple tuple = Tuple.newOne(topic);
		tuple.setField("name", identifier);
		tuple.setField("metric", metric);
		tuple.setField("yes", yes);
		tuple.setField("no", no);
		tuple.setField("timestamp", timestamp);
		return tuple;
	}

}
