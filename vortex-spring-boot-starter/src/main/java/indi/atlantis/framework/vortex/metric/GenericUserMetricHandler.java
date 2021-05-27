package indi.atlantis.framework.vortex.metric;

import indi.atlantis.framework.vortex.Handler;
import indi.atlantis.framework.vortex.common.Tuple;

/**
 * 
 * GenericUserMetricHandler
 * 
 * @author Fred Feng
 *
 * @version 1.0
 */
public class GenericUserMetricHandler<V> implements Handler {

	private final String topic;
	private final UserMetricListener<V> listener;

	public GenericUserMetricHandler(String topic, UserMetricListener<V> listener) {
		this.topic = topic;
		this.listener = listener;
	}

	@Override
	public void onData(Tuple tuple) {
		String name = tuple.getField("name", String.class);
		String metric = tuple.getField("metric", String.class);
		long timestamp = tuple.getTimestamp();
		listener.onMerge(name, metric, timestamp, tuple);
	}

	@Override
	public String getTopic() {
		return topic;
	}

}
