package indi.atlantis.framework.gearless.transport;

import java.net.SocketAddress;

/**
 * 
 * NioServer
 *
 * @author Jimmy Hoff
 * @version 1.0
 */
public interface NioServer {

	static final int PORT_RANGE_BEGIN = 50000;

	static final int PORT_RANGE_END = 60000;

	SocketAddress start() throws Exception;

	void stop();

	boolean isStarted();

}
