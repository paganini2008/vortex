package indi.atlantis.framework.vortex.metric;

/**
 * 
 * UserMetric
 *
 * @author Fred Feng
 * @version 1.0
 */
public interface UserMetric<V> extends Metric<UserMetric<V>> {

	V get();

}
