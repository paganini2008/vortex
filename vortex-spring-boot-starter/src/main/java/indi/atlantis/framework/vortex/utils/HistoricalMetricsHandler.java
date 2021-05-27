package indi.atlantis.framework.vortex.utils;

import indi.atlantis.framework.vortex.metric.Metric;

/**
 * 
 * HistoricalMetricsHandler
 *
 * @author Fred Feng
 * @version 1.0
 */
public interface HistoricalMetricsHandler<T extends Metric<T>> {

	void handleHistoricalMetrics(String metric, T metricUnit);

}
