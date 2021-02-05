package org.springtribe.framework.gearless.utils;

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
