package indi.atlantis.framework.vortex.metric;

import indi.atlantis.framework.tridenter.ApplicationInfo;

/**
 * 
 * SynchronizationExecutor
 * 
 * @author Jimmy Hoff
 *
 * @version 1.0
 */
public interface SynchronizationExecutor {

	void synchronizePeriodically(ApplicationInfo leaderInfo);

	void addSynchronizers(Synchronizer... synchronizers);

	void clearSynchronizers();

}