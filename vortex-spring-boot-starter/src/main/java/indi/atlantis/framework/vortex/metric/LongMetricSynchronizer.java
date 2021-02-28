package indi.atlantis.framework.vortex.metric;

import java.net.SocketAddress;
import java.util.Map;

import indi.atlantis.framework.vortex.common.NioClient;
import indi.atlantis.framework.vortex.common.Tuple;

/**
 * 
 * LongMetricSynchronizer
 *
 * @author Jimmy Hoff
 * @version 1.0
 */
public class LongMetricSynchronizer implements Synchronizer {

	private final String topic;
	private final MetricSequencer<String, NumberMetric<Long>> sequencer;
	private final boolean incremental;

	public LongMetricSynchronizer(String topic, MetricSequencer<String, NumberMetric<Long>> sequencer, boolean incremental) {
		this.topic = topic;
		this.sequencer = sequencer;
		this.incremental = incremental;
	}

	public void synchronize(NioClient nioClient, SocketAddress remoteAddress) {
		sequencer.scan((name, metric, data) -> {
			for (Map.Entry<String, NumberMetric<Long>> entry : data.entrySet()) {
				Tuple tuple = newTuple(name, metric, entry.getValue());
				nioClient.send(remoteAddress, tuple);
			}
		});
	}

	protected Tuple newTuple(String name, String metric, NumberMetric<Long> metricUnit) {
		long highestValue = metricUnit.getHighestValue().longValue();
		long lowestValue = metricUnit.getLowestValue().longValue();
		long totalValue = metricUnit.getTotalValue().longValue();
		long count = metricUnit.getCount();
		long timestamp = metricUnit.getTimestamp();

		Tuple tuple = Tuple.newOne(topic);
		tuple.setField("name", name);
		tuple.setField("metric", metric);
		tuple.setField("highestValue", highestValue);
		tuple.setField("lowestValue", lowestValue);
		tuple.setField("totalValue", totalValue);
		tuple.setField("count", count);
		tuple.setField("timestamp", timestamp);

		if (incremental) {
			sequencer.update(name, metric, timestamp,
					new NumberMetrics.LongMetric(highestValue, lowestValue, totalValue, count, timestamp, true), true);
		}
		return tuple;
	}

}
