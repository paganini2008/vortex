package io.atlantisframework.vortex.metric.api;

import com.github.paganini2008.devtools.collection.ConcurrentSortedBoundedMap;

import io.atlantisframework.vortex.metric.Metric;

/**
 * 
 * BoundedMetricMap
 *
 * @author Fred Feng
 * @since 2.0.4
 */
public class BoundedMetricMap<M, T extends Metric<T>> extends ConcurrentSortedBoundedMap<M, T> implements MetricMap<M, T> {

	private static final long serialVersionUID = 7381951980154645393L;

	public BoundedMetricMap(int maxSize, HistoricalMetricsHandler<M, T> historicalMetricsHandler) {
		super(new SimpleMetricMap<>(), maxSize);
		this.historicalMetricsHandler = historicalMetricsHandler;
	}

	private HistoricalMetricsHandler<M, T> historicalMetricsHandler;

	@Override
	public T merge(M key, T value) {
		return ((MetricMap<M, T>) getDelegate()).merge(key, value);
	}

	@Override
	public void onEviction(M eldestKey, T eldestValue) {
		if (historicalMetricsHandler != null) {
			historicalMetricsHandler.handleHistoricalMetrics(eldestKey, eldestValue);
		}
	}

}
