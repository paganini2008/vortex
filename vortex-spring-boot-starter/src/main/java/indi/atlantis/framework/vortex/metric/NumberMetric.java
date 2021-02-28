package indi.atlantis.framework.vortex.metric;

import java.util.Map;

/**
 * 
 * NumberMetric
 *
 * @author Jimmy Hoff
 * @version 1.0
 */
public interface NumberMetric<T extends Number> extends Metric<NumberMetric<T>> {

	T getHighestValue();

	T getLowestValue();

	T getTotalValue();

	long getCount();

	T getMiddleValue();

	Map<String, Object> toEntries();

}
