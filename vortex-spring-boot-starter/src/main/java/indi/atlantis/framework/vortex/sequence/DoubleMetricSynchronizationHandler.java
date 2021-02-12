package indi.atlantis.framework.vortex.sequence;

import indi.atlantis.framework.vortex.Handler;
import indi.atlantis.framework.vortex.common.Tuple;

/**
 * 
 * DoubleMetricSynchronizationHandler
 *
 * @author Jimmy Hoff
 * @version 1.0
 */
public class DoubleMetricSynchronizationHandler implements Handler {

	private final String topic;
	private final MetricSequencer<String, NumberMetric<Double>> sequencer;
	private final boolean merged;

	public DoubleMetricSynchronizationHandler(String topic, MetricSequencer<String, NumberMetric<Double>> sequencer, boolean merged) {
		this.topic = topic;
		this.sequencer = sequencer;
		this.merged = merged;
	}

	@Override
	public void onData(Tuple tuple) {
		String name = tuple.getField("name", String.class);
		String metric = tuple.getField("metric", String.class);
		Double highestValue = tuple.getField("highestValue", Double.class);
		Double lowestValue = tuple.getField("lowestValue", Double.class);
		Double totalValue = tuple.getField("totalValue", Double.class);
		long count = tuple.getField("count", Long.class);
		long timestamp = tuple.getTimestamp();
		sequencer.update(name, metric, timestamp,
				new NumberMetrics.DoubleMetric(highestValue, lowestValue, totalValue, count, timestamp, false), merged);
	}

	@Override
	public String getTopic() {
		return topic;
	}

}
