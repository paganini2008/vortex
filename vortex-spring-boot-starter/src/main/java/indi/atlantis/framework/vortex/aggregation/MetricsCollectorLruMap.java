package indi.atlantis.framework.vortex.aggregation;

import com.github.paganini2008.devtools.collection.LruMap;

import indi.atlantis.framework.vortex.utils.HistoricalMetricsHandler;

/**
 * 
 * MetricsCollectorLruMap
 *
 * @author Jimmy Hoff
 * @version 1.0
 */
public class MetricsCollectorLruMap<T extends Metric<T>> extends LruMap<String, T> {

	private static final long serialVersionUID = -3875714100550051178L;

	public MetricsCollectorLruMap(boolean ordered, int bufferSize, HistoricalMetricsHandler<T> historicalMetricsHandler) {
		super(new MetricsCollectorMap<T>(ordered), bufferSize);
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
