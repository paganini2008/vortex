package indi.atlantis.framework.vortex.transport;

import java.net.SocketAddress;

/**
 * 
 * NioServer
 *
 * @author Fred Feng
 * @version 1.0
 */
public interface NioServer {

	static final int PORT_RANGE_BEGIN = 50000;

	static final int PORT_RANGE_END = 60000;

	SocketAddress start() throws Exception;

	void stop();

	boolean isStarted();

}
