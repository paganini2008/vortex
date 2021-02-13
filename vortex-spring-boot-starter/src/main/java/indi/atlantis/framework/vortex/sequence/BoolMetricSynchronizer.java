package indi.atlantis.framework.vortex.sequence;

import java.net.SocketAddress;
import java.util.Map;

import indi.atlantis.framework.vortex.common.NioClient;
import indi.atlantis.framework.vortex.common.Tuple;

/**
 * 
 * BoolMetricSynchronizer
 *
 * @author Jimmy Hoff
 * @version 1.0
 */
public class BoolMetricSynchronizer implements Synchronizer {

	private final String topic;
	private final MetricSequencer<String, UserMetric<Bool>> sequencer;
	private final boolean incremental;

	public BoolMetricSynchronizer(String topic, MetricSequencer<String, UserMetric<Bool>> sequencer, boolean incremental) {
		this.topic = topic;
		this.sequencer = sequencer;
		this.incremental = incremental;
	}

	public void synchronize(NioClient nioClient, SocketAddress remoteAddress) {
		sequencer.scan((name, metric, data) -> {
			for (Map.Entry<String, UserMetric<Bool>> entry : data.entrySet()) {
				Tuple tuple = newTuple(name, metric, entry.getValue());
				nioClient.send(remoteAddress, tuple);
			}
		});
	}

	protected Tuple newTuple(String name, String metric, UserMetric<Bool> metricUnit) {
		long yes = metricUnit.get().getYes();
		long no = metricUnit.get().getNo();
		long timestamp = metricUnit.getTimestamp();
		Tuple tuple = Tuple.newOne(topic);
		tuple.setField("name", name);
		tuple.setField("metric", metric);
		tuple.setField("yes", yes);
		tuple.setField("no", no);
		tuple.setField("timestamp", timestamp);
		if (incremental) {
			sequencer.update(name, metric, timestamp, new BoolMetric(new Bool(yes, no), timestamp, true), true);
		}
		return tuple;
	}
}
