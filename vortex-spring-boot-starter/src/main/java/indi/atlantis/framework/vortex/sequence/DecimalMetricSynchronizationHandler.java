package indi.atlantis.framework.vortex.sequence;

import java.math.BigDecimal;

import indi.atlantis.framework.vortex.Handler;
import indi.atlantis.framework.vortex.common.Tuple;

/**
 * 
 * DecimalMetricSynchronizationHandler
 *
 * @author Jimmy Hoff
 * @version 1.0
 */
public class DecimalMetricSynchronizationHandler implements Handler {

	private final String topic;
	private final MetricSequencer<String, NumberMetric<BigDecimal>> sequencer;
	private final boolean merged;

	public DecimalMetricSynchronizationHandler(String topic, MetricSequencer<String, NumberMetric<BigDecimal>> sequencer, boolean merged) {
		this.topic = topic;
		this.sequencer = sequencer;
		this.merged = merged;
	}

	@Override
	public void onData(Tuple tuple) {
		String name = tuple.getField("name", String.class);
		String metric = tuple.getField("metric", String.class);
		BigDecimal highestValue = tuple.getField("highestValue", BigDecimal.class);
		BigDecimal lowestValue = tuple.getField("lowestValue", BigDecimal.class);
		BigDecimal totalValue = tuple.getField("totalValue", BigDecimal.class);
		long count = tuple.getField("count", Long.class);
		long timestamp = tuple.getTimestamp();
		sequencer.update(name, metric, timestamp,
				new NumberMetrics.DecimalMetric(highestValue, lowestValue, totalValue, count, timestamp, false), merged);
	}

	@Override
	public String getTopic() {
		return topic;
	}

}
