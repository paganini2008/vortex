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
public interface UserTypeHandler<K, V> {

	String getDataTypeName();

	UserMetric<V> convertAsMetric(K name, String metric, long timestamp, Tuple tuple);

	UserMetric<V> convertAsMetric(K name, String metric, long timestamp, UserMetric<V> metricUnit);

	Tuple convertAsTuple(String topic, K name, String metric, long timestamp, UserMetric<V> metricUnit);

}
