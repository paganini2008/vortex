package indi.atlantis.framework.vortex.common.embeddedio;

import java.net.SocketAddress;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.github.paganini2008.embeddedio.Channel;

import indi.atlantis.framework.vortex.common.ChannelContext;
import indi.atlantis.framework.vortex.common.Partitioner;

/**
 * 
 * EmbeddedChannelContext
 *
 * @author Fred Feng
 * @since 1.0
 */
public class EmbeddedChannelContext extends EmbeddedChannelContextAware implements ChannelContext<Channel> {

	private final List<Channel> holder = new CopyOnWriteArrayList<Channel>();

	public void addChannel(Channel channel, int weight) {
		for (int i = 0; i < weight; i++) {
			holder.add(channel);
		}
	}

	public Channel getChannel(SocketAddress address) {
		for (Channel channel : holder) {
			if (getRemoteAddress(channel) != null && getRemoteAddress(channel).equals(address)) {
				return channel;
			}
		}
		return null;
	}

	public void removeChannel(SocketAddress address) {
		for (Channel channel : holder) {
			if (getRemoteAddress(channel) != null && getRemoteAddress(channel).equals(address)) {
				holder.remove(channel);
			}
		}
	}

	private SocketAddress getRemoteAddress(Channel channel) {
		return channel.getRemoteAddr();
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
