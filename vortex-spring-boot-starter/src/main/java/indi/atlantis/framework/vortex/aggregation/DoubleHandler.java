package indi.atlantis.framework.vortex.aggregation;

import org.springframework.beans.factory.annotation.Autowired;

import indi.atlantis.framework.vortex.Handler;
import indi.atlantis.framework.vortex.common.Tuple;

/**
 * 
 * DoubleHandler
 *
 * @author Jimmy Hoff
 * @version 1.0
 */
public class DoubleHandler implements Handler {

	@Autowired
	private Sequencer<String, StatisticalMetric> sequencer;

	@Override
	public void onData(Tuple tuple) {
		String name = tuple.getField("name", String.class);
		String metric = tuple.getField("metric", String.class);
		double value = tuple.getField("value", Double.class);
		long timestamp = tuple.getTimestamp();
		sequencer.update(name, metric, timestamp, StatisticalMetrics.valueOf(value, timestamp));
	}

}
