package indi.atlantis.framework.vortex.aggregation;

/**
 * 
 * MetricEvictionHandler
 *
 * @author Jimmy Hoff
 * @version 1.0
 */
public interface MetricEvictionHandler<I, T extends Metric<T>> {

	void onEldestMetricRemoval(I identifier, String metric, T metricUnit);

}
