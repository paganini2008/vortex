package indi.atlantis.framework.vortex.metric;

import java.net.SocketAddress;

import indi.atlantis.framework.vortex.common.NioClient;

/**
 * 
 * Synchronizer
 *
 * @author Fred Feng
 * @version 1.0
 */
public interface Synchronizer {

	void synchronize(NioClient nioClient, SocketAddress remoteAddress);

}