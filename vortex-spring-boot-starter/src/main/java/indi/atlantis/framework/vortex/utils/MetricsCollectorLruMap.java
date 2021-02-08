package indi.atlantis.framework.vortex.utils;

import com.github.paganini2008.devtools.Console;
import com.github.paganini2008.devtools.collection.LruMap;

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
	
	public static void main(String[] args) {
		MetricsCollectorLruMap<StatisticalMetric> map = new MetricsCollectorLruMap<StatisticalMetric>(true, 10, null);
		for(int i=0;i<15;i++) {
			map.putIfAbsent(String.valueOf(i), StatisticalMetrics.valueOf(i, System.currentTimeMillis()));
		}
		Console.log(map);
	}

}
