package indi.atlantis.framework.vortex.sequence;

import indi.atlantis.framework.vortex.Handler;
import indi.atlantis.framework.vortex.common.Tuple;

/**
 * 
 * LongMetricSynchronizationHandler
 *
 * @author Jimmy Hoff
 * @version 1.0
 */
public class LongMetricSynchronizationHandler implements Handler {

	private final String topic;
	private final MetricSequencer<String, NumberMetric<Long>> sequencer;
	private final boolean merged;

	public LongMetricSynchronizationHandler(String topic, MetricSequencer<String, NumberMetric<Long>> sequencer, boolean merged) {
		this.topic = topic;
		this.sequencer = sequencer;
		this.merged = merged;
	}

	@Override
	public void onData(Tuple tuple) {
		String name = tuple.getField("name", String.class);
		String metric = tuple.getField("metric", String.class);
		long highestValue = tuple.getField("highestValue", Long.class);
		long lowestValue = tuple.getField("lowestValue", Long.class);
		long totalValue = tuple.getField("totalValue", Long.class);
		long count = tuple.getField("count", Long.class);
		long timestamp = tuple.getTimestamp();
		sequencer.update(name, metric, timestamp,
				new NumberMetrics.LongMetric(highestValue, lowestValue, totalValue, count, timestamp, false), merged);
	}

	@Override
	public String getTopic() {
		return topic;
	}

}
