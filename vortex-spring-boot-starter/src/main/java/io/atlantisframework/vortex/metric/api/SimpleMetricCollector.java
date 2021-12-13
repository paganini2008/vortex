package io.atlantisframework.vortex.metric.api;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import io.atlantisframework.vortex.metric.Metric;

/**
 * 
 * SimpleMetricCollector
 *
 * @author Fred Feng
 * @since 2.0.4
 */
public class SimpleMetricCollector<M, T extends Metric<T>> implements MetricCollector<M, T> {

	private final MetricMap<M, T> store;

	public SimpleMetricCollector(int bufferSize, HistoricalMetricsHandler<M, T> historicalMetricsHandler) {
		this.store = bufferSize > 0 ? new BoundedMetricMap<>(bufferSize, historicalMetricsHandler) : new SimpleMetricMap<>();
	}

	@Override
	public T set(M metric, T metricUnit, boolean merged) {
		return merged ? store.merge(metric, metricUnit) : store.put(metric, metricUnit);
	}

	@Override
	public T get(M metric) {
		return store.get(metric);
	}

	@Override
	public Collection<M> metrics() {
		return Collections.unmodifiableCollection(store.keySet());
	}

	@Override
	public Map<M, T> all() {
		return Collections.unmodifiableMap(new LinkedHashMap<M, T>(store));
	}

	@Override
	public int size() {
		return store.size();
	}

	@Override
	public void clear() {
		store.clear();
	}

}
