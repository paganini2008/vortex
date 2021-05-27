package indi.atlantis.framework.vortex.common;

import java.net.SocketAddress;

/**
 * 
 * Client
 * 
 * @author Fred Feng
 *
 * @since 1.0
 */
public interface Client {

	void send(Object data);

	void send(SocketAddress socketAddress, Object data);

	void send(Object data, Partitioner partitioner);

}
