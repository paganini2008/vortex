package indi.atlantis.framework.vortex.metric;

import indi.atlantis.framework.vortex.common.Tuple;

/**
 * 
 * UserMetricListener
 * 
 * @author Fred Feng
 *
 * @version 1.0
 */
public interface UserMetricListener<V> {

	void onMerge(String identifier, String metric, long timestamp, Tuple tuple);

	void onReset(String identifier, String metric, long timestamp, UserMetric<V> metricUnit);

	void onSync(String identifier, String metric, long timestamp, Tuple tuple, boolean merged);

	UserTypeHandler<V> getTypeHandler();

	MetricSequencer<String, UserMetric<V>> getMetricSequencer();

}