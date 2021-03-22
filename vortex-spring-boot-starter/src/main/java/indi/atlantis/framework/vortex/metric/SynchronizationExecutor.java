package indi.atlantis.framework.vortex.metric;

/**
 * 
 * SynchronizationExecutor
 * 
 * @author Jimmy Hoff
 *
 * @version 1.0
 */
public interface SynchronizationExecutor {

	void addSynchronizers(Synchronizer... synchronizers);

	void clearSynchronizers();

}