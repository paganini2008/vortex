package indi.atlantis.framework.gearless.common.grizzly;

import java.net.SocketAddress;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.glassfish.grizzly.Connection;

import indi.atlantis.framework.gearless.common.ChannelContext;
import indi.atlantis.framework.gearless.common.Partitioner;

/**
 * 
 * GrizzlyChannelContext
 *
 * @author Jimmy Hoff
 * @version 1.0
 */
public class GrizzlyChannelContext extends GrizzlyChannelContextAware implements ChannelContext<Connection<?>> {

	private final List<Connection<?>> holder = new CopyOnWriteArrayList<Connection<?>>();

	public void addChannel(Connection<?> channel, int weight) {
		for (int i = 0; i < weight; i++) {
			holder.add(channel);
		}
	}

	public Connection<?> getChannel(SocketAddress address) {
		for (Connection<?> channel : holder) {
			if (getRemoteAddress(channel) != null && getRemoteAddress(channel).equals(address)) {
				return channel;
			}
		}
		return null;
	}

	public void removeChannel(SocketAddress address) {
		for (Connection<?> channel : holder) {
			if (getRemoteAddress(channel) != null && getRemoteAddress(channel).equals(address)) {
				holder.remove(channel);
			}
		}
	}

	private SocketAddress getRemoteAddress(Connection<?> channel) {
		return (SocketAddress) channel.getPeerAddress();
	}

	public int countOfChannels() {
		return holder.size();
	}

	public Connection<?> selectChannel(Object data, Partitioner partitioner) {
		return holder.isEmpty() ? null : partitioner.selectChannel(data, holder);
	}

	public Collection<Connection<?>> getChannels() {
		return holder;
	}
}
