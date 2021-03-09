package indi.atlantis.framework.vortex.metric;

/**
 * 
 * UserMetric
 *
 * @author Jimmy Hoff
 * @version 1.0
 */
public interface UserMetric<V> extends Metric<UserMetric<V>> {

	V get();

}
