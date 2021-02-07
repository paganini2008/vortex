package indi.atlantis.framework.gearless.utils;

import java.util.Map;

/**
 * 
 * StatisticalMetric
 *
 * @author Jimmy Hoff
 * @version 1.0
 */
public interface StatisticalMetric extends Metric<StatisticalMetric> {

	Number getHighestValue();

	Number getLowestValue();

	Number getTotalValue();

	long getCount();

	Number getMiddleValue(int scale);

	Map<String, Object> toEntries();

}
