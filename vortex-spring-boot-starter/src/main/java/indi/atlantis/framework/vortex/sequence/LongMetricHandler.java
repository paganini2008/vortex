package indi.atlantis.framework.vortex.sequence;

import indi.atlantis.framework.vortex.Handler;
import indi.atlantis.framework.vortex.common.Tuple;

/**
 * 
 * LongMetricHandler
 *
 * @author Jimmy Hoff
 * @version 1.0
 */
public class LongMetricHandler implements Handler {

	private final String topic;
	private final MetricSequencer<String, NumberMetric<Long>> sequencer;

	public LongMetricHandler(String topic, MetricSequencer<String, NumberMetric<Long>> sequencer) {
		this.topic = topic;
		this.sequencer = sequencer;
	}

	@Override
	public void onData(Tuple tuple) {
		String name = tuple.getField("name", String.class);
		String metric = tuple.getField("metric", String.class);
		long value = tuple.getField("value", Long.class);
		long timestamp = tuple.getTimestamp();
		sequencer.update(name, metric, timestamp, new NumberMetrics.LongMetric(value, timestamp), true);
	}

	@Override
	public String getTopic() {
		return topic;
	}

}
