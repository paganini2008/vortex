package indi.atlantis.framework.vortex.sequence;

import com.github.paganini2008.devtools.collection.LruMap;

import indi.atlantis.framework.vortex.utils.HistoricalMetricsHandler;

/**
 * 
 * MetricCollectorLruMap
 *
 * @author Jimmy Hoff
 * @version 1.0
 */
public class MetricCollectorLruMap<T extends Metric<T>> extends LruMap<String, T> {

	private static final long serialVersionUID = -3875714100550051178L;

	public MetricCollectorLruMap(boolean ordered, int bufferSize, HistoricalMetricsHandler<T> historicalMetricsHandler) {
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
