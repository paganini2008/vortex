package indi.atlantis.framework.gearless.utils;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 
 * SimpleMetricsCollector
 *
 * @author Jimmy Hoff
 * @version 1.0
 */
public class SimpleMetricsCollector<T extends Metric<T>> implements MetricsCollector<T> {

	private final Map<String, T> store;

	public SimpleMetricsCollector() {
		this(-1, null);
	}

	public SimpleMetricsCollector(int bufferSize, HistoricalMetricsHandler<T> historicalMetricsHandler) {
		this(true, bufferSize, historicalMetricsHandler);
	}

	public SimpleMetricsCollector(boolean ordered, int bufferSize, HistoricalMetricsHandler<T> historicalMetricsHandler) {
		this.store = bufferSize > 0 ? new ScrollingMetricsCollectorMap<T>(ordered, bufferSize, historicalMetricsHandler)
				: new MetricsCollectorMap<T>(ordered);
	}

	@Override
	public T set(String metric, T metricUnit) {
		return store.put(metric, metricUnit);
	}

	@Override
	public T get(String metric) {
		return store.get(metric);
	}

	@Override
	public String[] metrics() {
		return store.keySet().toArray(new String[0]);
	}

	@Override
	public Map<String, T> all() {
		return Collections.unmodifiableMap(new LinkedHashMap<String, T>(store));
	}

	@Override
	public int size() {
		return store.size();
	}
	
	

}
