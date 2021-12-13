package io.atlantisframework.vortex.metric.api;

import java.util.Collection;
import java.util.Map;

import io.atlantisframework.vortex.metric.Metric;

/**
 * 
 * MetricCollector
 *
 * @author Fred Feng
 * @since 2.0.4
 */
public interface MetricCollector<M, T extends Metric<T>> {

	T set(M metric, T metricUnit, boolean merged);

	T get(M metric);

	Collection<M> metrics();

	Map<M, T> all();

	int size();

	void clear();

}
