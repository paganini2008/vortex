package indi.atlantis.framework.vortex.utils;

/**
 * 
 * Metric
 *
 * @author Jimmy Hoff
 * @version 1.0
 */
public interface Metric<T extends Metric<T>> {

	long getTimestamp();

	boolean reset();

	T reset(T currentMetric);

	T merge(T anotherMetric);
}
