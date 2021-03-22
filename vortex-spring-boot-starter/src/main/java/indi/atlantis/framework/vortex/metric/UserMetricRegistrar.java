package indi.atlantis.framework.vortex.metric;

import indi.atlantis.framework.vortex.Handler;

/**
 * 
 * UserMetricRegistrar
 * 
 * @author Jimmy Hoff
 *
 * @version 1.0
 */
public interface UserMetricRegistrar<V> {
	
	String getDataType();

	MetricSequencer<String, UserMetric<V>> getMetricSequencer();

	Handler getHandler();

	Handler getSynchronizationHandler();

	Handler getIncrementalSynchronizationHandler();

	Synchronizer getSynchronizer();

	Synchronizer getIncrementalSynchronizer();

}
