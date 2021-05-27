package indi.atlantis.framework.vortex.common;

import java.net.SocketAddress;
import java.util.Collection;

/**
 * 
 * ChannelContext
 * 
 * @author Fred Feng
 * @version 1.0
 */
public interface ChannelContext<T> {

	default void addChannel(T channel) {
		addChannel(channel, 1);
	}

	void addChannel(T channel, int weight);

	T getChannel(SocketAddress address);

	void removeChannel(SocketAddress address);

	int countOfChannels();

	T selectChannel(Object data, Partitioner partitioner);

	Collection<T> getChannels();

	void setChannelEventListener(ChannelEventListener<T> channelEventListener);

}