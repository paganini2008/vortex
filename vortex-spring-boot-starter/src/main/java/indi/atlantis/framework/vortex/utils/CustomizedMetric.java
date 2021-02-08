package indi.atlantis.framework.vortex.utils;

/**
 * 
 * CustomizedMetric
 *
 * @author Jimmy Hoff
 * @version 1.0
 */
public interface CustomizedMetric<T> extends Metric<CustomizedMetric<T>> {

	T get();

}
