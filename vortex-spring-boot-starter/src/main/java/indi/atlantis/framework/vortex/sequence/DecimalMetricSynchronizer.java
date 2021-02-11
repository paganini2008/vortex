package indi.atlantis.framework.vortex.sequence;

import java.math.BigDecimal;
import java.net.SocketAddress;
import java.util.Map;

import indi.atlantis.framework.vortex.common.NioClient;
import indi.atlantis.framework.vortex.common.Tuple;

/**
 * 
 * DecimalMetricSynchronizer
 *
 * @author Jimmy Hoff
 * @version 1.0
 */
public class DecimalMetricSynchronizer implements Synchronizer {

	private final String topic;
	private final Sequencer<String, NumberMetric<BigDecimal>> sequencer;
	private final boolean incremental;

	public DecimalMetricSynchronizer(String topic, Sequencer<String, NumberMetric<BigDecimal>> sequencer, boolean incremental) {
		this.topic = topic;
		this.sequencer = sequencer;
		this.incremental = incremental;
	}

	public void synchronize(NioClient nioClient, SocketAddress remoteAddress) {
		sequencer.scan((name, metric, data) -> {
			for (Map.Entry<String, NumberMetric<BigDecimal>> entry : data.entrySet()) {
				Tuple tuple = newTuple(name, metric, entry.getValue());
				nioClient.send(remoteAddress, tuple);
			}
		});
	}

	protected Tuple newTuple(String name, String metric, NumberMetric<BigDecimal> metricUnit) {
		BigDecimal highestValue = metricUnit.getHighestValue();
		BigDecimal lowestValue = metricUnit.getLowestValue();
		BigDecimal totalValue = metricUnit.getTotalValue();
		long count = metricUnit.getCount();
		long timestamp = metricUnit.getTimestamp();

		Tuple tuple = Tuple.newOne(topic);
		tuple.setField("highestValue", highestValue);
		tuple.setField("lowestValue", lowestValue);
		tuple.setField("totalValue", totalValue);
		tuple.setField("count", count);
		tuple.setField("timestamp", timestamp);

		if (incremental) {
			sequencer.update(name, metric, timestamp,
					new NumberMetrics.DecimalMetric(highestValue, lowestValue, totalValue, count, timestamp, true), true);
		}
		return tuple;
	}

}
