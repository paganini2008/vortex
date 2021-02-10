package indi.atlantis.framework.vortex.sequence;

/**
 * 
 * UserMetric
 *
 * @author Jimmy Hoff
 * @version 1.0
 */
public interface UserMetric<T> extends Metric<UserMetric<T>> {

	T get();

}
