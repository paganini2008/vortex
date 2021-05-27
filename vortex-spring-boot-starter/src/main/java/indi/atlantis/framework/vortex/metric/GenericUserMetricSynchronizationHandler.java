package indi.atlantis.framework.vortex.metric;

import indi.atlantis.framework.vortex.Handler;
import indi.atlantis.framework.vortex.common.Tuple;

/**
 * 
 * GenericUserMetricSynchronizationHandler
 * 
 * @author Fred Feng
 *
 * @version 1.0
 */
public class GenericUserMetricSynchronizationHandler<V> implements Handler {

	private final String topic;
	private final UserMetricListener<V> listener;
	private final boolean merged;

	public GenericUserMetricSynchronizationHandler(String topic, boolean merged, UserMetricListener<V> listener) {
		this.topic = topic;
		this.merged = merged;
		this.listener = listener;
	}

	@Override
	public void onData(Tuple tuple) {
		String name = tuple.getField("name", String.class);
		String metric = tuple.getField("metric", String.class);
		long timestamp = tuple.getTimestamp();
		listener.onSync(name, metric, timestamp, tuple, merged);
	}

	@Override
	public String getTopic() {
		return topic;
	}

}
