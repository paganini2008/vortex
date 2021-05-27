package indi.atlantis.framework.vortex.metric;

import indi.atlantis.framework.tridenter.ApplicationInfo;

/**
 * 
 * SynchronizationExecutor
 * 
 * @author Fred Feng
 *
 * @version 1.0
 */
public interface SynchronizationExecutor {

	void synchronizePeriodically(ApplicationInfo leaderInfo);

	void addSynchronizers(Synchronizer... synchronizers);

	void clearSynchronizers();

}