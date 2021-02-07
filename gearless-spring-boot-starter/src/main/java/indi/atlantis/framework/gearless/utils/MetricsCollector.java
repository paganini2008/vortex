package indi.atlantis.framework.gearless.utils;

import java.util.Map;

/**
 * 
 * MetricsCollector
 *
 * @author Jimmy Hoff
 * @version 1.0
 */
public interface MetricsCollector<T extends Metric<T>> {

	T set(String metric, T metricUnit);

	T get(String metric);

	String[] metrics();

	Map<String, T> all();
	
	int size();

}