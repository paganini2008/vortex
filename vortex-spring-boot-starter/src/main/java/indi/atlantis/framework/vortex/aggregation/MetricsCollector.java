package indi.atlantis.framework.vortex.aggregation;

import java.util.Map;

/**
 * 
 * MetricsCollector
 *
 * @author Jimmy Hoff
 * @version 1.0
 */
public interface MetricsCollector<T extends Metric<T>> {

	T set(String metric, T metricUnit, boolean merged);

	T get(String metric);

	String[] metrics();

	Map<String, T> all();

	int size();

	void clear();

}