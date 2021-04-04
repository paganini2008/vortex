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
public class GenericUserMetricListener<V> implements UserMetricListener<V> {

	private final MetricSequencer<String, UserMetric<V>> sequencer;
	private final UserTypeHandler<V> typeHandler;
	
	public GenericUserMetricListener(MetricSequencer<String, UserMetric<V>> sequencer, UserTypeHandler<V> typeHandler) {
		this.sequencer = sequencer;
		this.typeHandler = typeHandler;
	}

	public GenericUserMetricListener(UserMetricSequencer<String, V> sequencer, UserTypeHandler<V> typeHandler) {
		this.sequencer = sequencer;
		this.typeHandler = typeHandler;
	}

	@Override
	public void onMerge(String identifier, String metric, long timestamp, Tuple tuple) {
		UserMetric<V> userMetric = typeHandler.convertAsMetric(identifier, metric, timestamp, tuple);
		if (userMetric != null) {
			sequencer.update(identifier, metric, timestamp, userMetric, true);
		}
	}

	@Override
	public void onReset(String identifier, String metric, long timestamp, UserMetric<V> metricUnit) {
		UserMetric<V> userMetric = typeHandler.convertAsMetric(identifier, metric, timestamp, metricUnit);
		if (userMetric != null) {
			sequencer.update(identifier, metric, timestamp, new ResettableUserMetric<V>(userMetric), true);
		}
	}

	@Override
	public void onSync(String identifier, String metric, long timestamp, Tuple tuple, boolean merged) {
		UserMetric<V> userMetric = typeHandler.convertAsMetric(identifier, metric, timestamp, tuple);
		if (userMetric != null) {
			sequencer.update(identifier, metric, timestamp, userMetric, merged);
		}
	}

	@Override
	public MetricSequencer<String, UserMetric<V>> getMetricSequencer() {
		return sequencer;
	}

	@Override
	public UserTypeHandler<V> getTypeHandler() {
		return typeHandler;
	}

}
