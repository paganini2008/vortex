package indi.atlantis.framework.vortex.metric;

import java.util.Map;

/**
 * 
 * Metric
 *
 * @author Fred Feng
 * @version 1.0
 */
public interface Metric<T extends Metric<T>> {

	long getTimestamp();

	boolean reset();

	T reset(T newMetric);

	T merge(T newMetric);

	Map<String, Object> toEntries();
}
