package indi.atlantis.framework.vortex.utils;

import indi.atlantis.framework.vortex.sequence.Metric;

/**
 * 
 * HistoricalMetricsHandler
 *
 * @author Jimmy Hoff
 * @version 1.0
 */
public interface HistoricalMetricsHandler<T extends Metric<T>> {

	void handleHistoricalMetrics(String metric, T metricUnit);

}
