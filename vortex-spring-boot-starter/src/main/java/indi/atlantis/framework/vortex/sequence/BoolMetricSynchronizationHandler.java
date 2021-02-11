package indi.atlantis.framework.vortex.sequence;

import indi.atlantis.framework.vortex.Handler;
import indi.atlantis.framework.vortex.common.Tuple;

/**
 * 
 * BoolMetricSynchronizationHandler
 *
 * @author Jimmy Hoff
 * @version 1.0
 */
public class BoolMetricSynchronizationHandler implements Handler {

	private final String topic;
	private final Sequencer<String, UserMetric<Bool>> sequencer;
	private final boolean merged;

	public BoolMetricSynchronizationHandler(String topic, Sequencer<String, UserMetric<Bool>> sequencer, boolean merged) {
		this.topic = topic;
		this.sequencer = sequencer;
		this.merged = merged;
	}

	@Override
	public void onData(Tuple tuple) {
		String name = tuple.getField("name", String.class);
		String metric = tuple.getField("metric", String.class);
		long yes = tuple.getField("yes", Long.class);
		long no = tuple.getField("no", Long.class);
		long timestamp = tuple.getTimestamp();
		sequencer.update(name, metric, timestamp, new BoolMetric(new Bool(yes, no), timestamp, false), merged);
	}

	@Override
	public String getTopic() {
		return topic;
	}

}
