package indi.atlantis.framework.vortex.metric;

import indi.atlantis.framework.vortex.Handler;
import indi.atlantis.framework.vortex.common.Tuple;

/**
 * 
 * BoolMetricHandler
 *
 * @author Jimmy Hoff
 * @version 1.0
 */
public class BoolMetricHandler implements Handler {

	private final String topic;
	private final MetricSequencer<String, UserMetric<Bool>> sequencer;

	public BoolMetricHandler(String topic, MetricSequencer<String, UserMetric<Bool>> sequencer) {
		this.topic = topic;
		this.sequencer = sequencer;
	}

	@Override
	public void onData(Tuple tuple) {
		String name = tuple.getField("name", String.class);
		String metric = tuple.getField("metric", String.class);
		boolean value = tuple.getField("value", Boolean.class);
		long timestamp = tuple.getTimestamp();
		sequencer.update(name, metric, timestamp, new BoolMetric(value, timestamp), true);
	}

	@Override
	public String getTopic() {
		return topic;
	}

}
