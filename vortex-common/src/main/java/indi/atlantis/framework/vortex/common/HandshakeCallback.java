package indi.atlantis.framework.vortex.common;

import java.net.SocketAddress;

/**
 * 
 * HandshakeCallback
 * 
 * @author Jimmy Hoff
 * 
 * 
 * @version 1.0
 */
@FunctionalInterface
public interface HandshakeCallback {

	void operationComplete(SocketAddress address);

}
