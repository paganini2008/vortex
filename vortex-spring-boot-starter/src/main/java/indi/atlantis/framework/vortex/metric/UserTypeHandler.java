package indi.atlantis.framework.vortex.metric;

import indi.atlantis.framework.vortex.common.Tuple;

/**
 * 
 * UserTypeHandler
 * 
 * @author Jimmy Hoff
 *
 * @version 1.0
 */
public interface UserTypeHandler<V> {

	String getDataTypeName();

	UserMetric<V> convertAsMetric(String identifier, String metric, long timestamp, Tuple tuple);

	UserMetric<V> convertAsMetric(String identifier, String metric, long timestamp, UserMetric<V> metricUnit);

	Tuple convertAsTuple(String topic, String identifier, String metric, long timestamp, UserMetric<V> metricUnit);

}
