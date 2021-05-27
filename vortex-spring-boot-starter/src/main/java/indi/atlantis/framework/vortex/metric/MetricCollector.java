package indi.atlantis.framework.vortex.metric;

import java.util.Map;

/**
 * 
 * MetricCollector
 *
 * @author Fred Feng
 * @version 1.0
 */
public interface MetricCollector<T extends Metric<T>> {

	T set(String metric, T metricUnit, boolean merged);

	T get(String metric);

	String[] metrics();

	Map<String, T> all();

	int size();

	void clear();

}