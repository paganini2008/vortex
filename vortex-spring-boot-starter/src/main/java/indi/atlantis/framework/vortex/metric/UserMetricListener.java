package indi.atlantis.framework.vortex.metric;

import indi.atlantis.framework.vortex.common.Tuple;

/**
 * 
 * UserMetricListener
 * 
 * @author Jimmy Hoff
 *
 * @version 1.0
 */
public interface UserMetricListener<K, V> {

	void onMerge(K name, String metric, long timestamp, Tuple tuple);

	void onReset(K name, String metric, long timestamp, UserMetric<V> metricUnit);

	void onSync(K name, String metric, long timestamp, Tuple tuple, boolean merged);

	UserTypeHandler<K, V> getTypeHandler();

	MetricSequencer<K, UserMetric<V>> getMetricSequencer();

}