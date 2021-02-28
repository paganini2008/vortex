package indi.atlantis.framework.vortex.metric;

import com.github.paganini2008.devtools.collection.SimpleBoundedMap;

import indi.atlantis.framework.vortex.utils.HistoricalMetricsHandler;

/**
 * 
 * MetricCollectorBoundedMap
 *
 * @author Jimmy Hoff
 * @version 1.0
 */
public class MetricCollectorBoundedMap<T extends Metric<T>> extends SimpleBoundedMap<String, T> {

	private static final long serialVersionUID = -3875714100550051178L;

	public MetricCollectorBoundedMap(boolean ordered, int bufferSize, HistoricalMetricsHandler<T> historicalMetricsHandler) {
		super(new MetricCollectorMap<T>(ordered), bufferSize);
		this.historicalMetricsHandler = historicalMetricsHandler;
	}

	private HistoricalMetricsHandler<T> historicalMetricsHandler;

	@Override
	public void onEviction(String metric, T metricUnit) {
		if (historicalMetricsHandler != null) {
			historicalMetricsHandler.handleHistoricalMetrics(metric, metricUnit);
		}
	}

}
