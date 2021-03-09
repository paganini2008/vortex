package indi.atlantis.framework.vortex.metric;

import indi.atlantis.framework.vortex.common.Tuple;

/**
 * 
 * GenericUserMetricListener
 * 
 * @author Jimmy Hoff
 *
 * @version 1.0
 */
public class GenericUserMetricListener<K, V> implements UserMetricListener<K, V> {

	private final MetricSequencer<K, UserMetric<V>> sequencer;
	private final UserTypeHandler<K, V> typeHandler;

	public GenericUserMetricListener(MetricSequencer<K, UserMetric<V>> sequencer, UserTypeHandler<K, V> typeHandler) {
		this.sequencer = sequencer;
		this.typeHandler = typeHandler;
	}

	@Override
	public void onMerge(K name, String metric, long timestamp, Tuple tuple) {
		UserMetric<V> userMetric = typeHandler.convertAsMetric(name, metric, timestamp, tuple);
		if (userMetric != null) {
			sequencer.update(name, metric, timestamp, userMetric, true);
		}
	}

	@Override
	public void onReset(K name, String metric, long timestamp, UserMetric<V> metricUnit) {
		UserMetric<V> userMetric = typeHandler.convertAsMetric(name, metric, timestamp, metricUnit);
		if (userMetric != null) {
			sequencer.update(name, metric, timestamp, new ResettableUserMetric<V>(userMetric), true);
		}
	}

	@Override
	public void onSync(K name, String metric, long timestamp, Tuple tuple, boolean merged) {
		UserMetric<V> userMetric = typeHandler.convertAsMetric(name, metric, timestamp, tuple);
		if (userMetric != null) {
			sequencer.update(name, metric, timestamp, userMetric, merged);
		}
	}

	@Override
	public MetricSequencer<K, UserMetric<V>> getMetricSequencer() {
		return sequencer;
	}

	@Override
	public UserTypeHandler<K, V> getTypeHandler() {
		return typeHandler;
	}

}
