package indi.atlantis.framework.vortex.common;

import java.util.concurrent.TimeUnit;

/**
 * 
 * NioClient
 * 
 * @author Fred Feng
 * @version 1.0
 */
public interface NioClient extends LifeCycle, NioConnection, Client {

	void setThreadCount(int threadCount);

	void watchConnection(int checkInterval, TimeUnit timeUnit);

	void setIdleTimeout(int idleTimeout);

}
