package indi.atlantis.framework.vortex.sequence;

import java.math.BigDecimal;

import indi.atlantis.framework.vortex.Handler;
import indi.atlantis.framework.vortex.common.Tuple;

/**
 * 
 * DecimalMetricHandler
 *
 * @author Jimmy Hoff
 * @version 1.0
 */
public class DecimalMetricHandler implements Handler {

	private final String topic;
	private final Sequencer<String, NumberMetric<BigDecimal>> sequencer;

	public DecimalMetricHandler(String topic, Sequencer<String, NumberMetric<BigDecimal>> sequencer) {
		this.topic = topic;
		this.sequencer = sequencer;
	}

	@Override
	public void onData(Tuple tuple) {
		String name = tuple.getField("name", String.class);
		String metric = tuple.getField("metric", String.class);
		BigDecimal value = tuple.getField("value", BigDecimal.class);
		long timestamp = tuple.getTimestamp();
		sequencer.update(name, metric, timestamp, new NumberMetrics.DecimalMetric(value, timestamp), true);
	}

	@Override
	public String getTopic() {
		return topic;
	}

}
