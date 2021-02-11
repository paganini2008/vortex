package indi.atlantis.framework.vortex.sequence;

import indi.atlantis.framework.vortex.Handler;
import indi.atlantis.framework.vortex.common.Tuple;

/**
 * 
 * DoubleMetricHandler
 *
 * @author Jimmy Hoff
 * @version 1.0
 */
public class DoubleMetricHandler implements Handler {

	private final String topic;
	private final Sequencer<String, NumberMetric<Double>> sequencer;

	public DoubleMetricHandler(String topic, Sequencer<String, NumberMetric<Double>> sequencer) {
		this.topic = topic;
		this.sequencer = sequencer;
	}

	@Override
	public void onData(Tuple tuple) {
		String name = tuple.getField("name", String.class);
		String metric = tuple.getField("metric", String.class);
		Double value = tuple.getField("value", Double.class);
		long timestamp = tuple.getTimestamp();
		sequencer.update(name, metric, timestamp, new NumberMetrics.DoubleMetric(value, timestamp), true);
	}

	@Override
	public String getTopic() {
		return topic;
	}

}
