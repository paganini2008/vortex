package io.atlantisframework.vortex.metric.api;

import java.util.Map;

import io.atlantisframework.vortex.metric.Metric;

/**
 * 
 * MetricMap
 *
 * @author Fred Feng
 * @since 2.0.4
 */
public interface MetricMap<M, T extends Metric<T>> extends Map<M, T> {

	T merge(M key, T value);

}