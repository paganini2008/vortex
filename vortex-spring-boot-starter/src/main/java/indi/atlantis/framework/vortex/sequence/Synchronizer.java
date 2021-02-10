package indi.atlantis.framework.vortex.sequence;

import java.net.SocketAddress;

import indi.atlantis.framework.vortex.common.NioClient;

/**
 * 
 * Synchronizer
 *
 * @author Jimmy Hoff
 * @version 1.0
 */
public interface Synchronizer {

	void synchronize(NioClient nioClient, SocketAddress remoteAddress);

}