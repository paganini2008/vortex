package org.springtribe.framework.gearless.common.netty;

import java.net.SocketAddress;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springtribe.framework.gearless.common.ChannelContext;
import org.springtribe.framework.gearless.common.Partitioner;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler.Sharable;
import lombok.extern.slf4j.Slf4j;

/**
 * 
 * NettyChannelContext
 * 
 * @author Jimmy Hoff
 * 
 * 
 * @version 1.0
 */
@Slf4j
@Sharable
public class NettyChannelContext extends NettyChannelContextAware implements ChannelContext<Channel> {

	private final List<Channel> holder = new CopyOnWriteArrayList<Channel>();

	public void addChannel(Channel channel, int weight) {
		for (int i = 0; i < weight; i++) {
			holder.add(channel);
		}
		if (log.isTraceEnabled()) {
			log.trace("Current channel size: " + countOfChannels());
		}
	}

	public Channel getChannel(SocketAddress address) {
		for (Channel channel : holder) {
			if (channel.remoteAddress() != null && channel.remoteAddress().equals(address)) {
				return channel;
			}
		}
		return null;
	}

	public void removeChannel(SocketAddress address) {
		for (Channel channel : holder) {
			if (channel.remoteAddress() != null && channel.remoteAddress().equals(address)) {
				holder.remove(channel);
			}
		}
	}

	public int countOfChannels() {
		return holder.size();
	}

	public Channel selectChannel(Object data, Partitioner partitioner) {
		return holder.isEmpty() ? null : partitioner.selectChannel(data, holder);
	}

	public Collection<Channel> getChannels() {
		return holder;
	}

}
