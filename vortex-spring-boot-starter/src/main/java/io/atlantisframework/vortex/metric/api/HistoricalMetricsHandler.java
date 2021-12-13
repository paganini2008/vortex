package io.atlantisframework.vortex.metric.api;

import io.atlantisframework.vortex.metric.Metric;

/**
 * 
 * HistoricalMetricsHandler
 *
 * @author Fred Feng
 * @since 2.0.4
 */
public interface HistoricalMetricsHandler<M, T extends Metric<T>> {

	void handleHistoricalMetrics(M metric, T metricUnit);

}
