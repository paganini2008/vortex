package indi.atlantis.framework.vortex.metric;

import java.util.Map;

/**
 * 
 * UserMetric
 *
 * @author Jimmy Hoff
 * @version 1.0
 */
public interface UserMetric<T> extends Metric<UserMetric<T>> {

	T get();

	Map<String, Object> toEntries();

}
