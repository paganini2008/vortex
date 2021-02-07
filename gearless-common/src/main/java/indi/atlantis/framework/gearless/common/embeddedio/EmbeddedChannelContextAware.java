package indi.atlantis.framework.gearless.common.embeddedio;

import java.io.IOException;
import java.net.SocketAddress;

import com.github.paganini2008.embeddedio.Channel;
import com.github.paganini2008.embeddedio.ChannelHandler;
import com.github.paganini2008.embeddedio.MessagePacket;

import indi.atlantis.framework.gearless.common.ChannelContext;
import indi.atlantis.framework.gearless.common.ChannelEvent;
import indi.atlantis.framework.gearless.common.ChannelEventListener;
import indi.atlantis.framework.gearless.common.ConnectionWatcher;
import indi.atlantis.framework.gearless.common.Tuple;
import indi.atlantis.framework.gearless.common.ChannelEvent.EventType;

/**
 * 
 * EmbeddedChannelContextAware
 *
 * @author Jimmy Hoff
 * @since 1.0
 */
public abstract class EmbeddedChannelContextAware implements ChannelHandler, ChannelContext<Channel> {

	private ConnectionWatcher connectionWatcher;
	private ChannelEventListener<Channel> channelEventListener;

	public ConnectionWatcher getConnectionWatcher() {
		return connectionWatcher;
	}

	public void setConnectionWatcher(ConnectionWatcher connectionWatcher) {
		this.connectionWatcher = connectionWatcher;
	}

	@Override
	public void fireChannelActive(Channel channel) throws IOException {
		addChannel(channel);
		fireChannelEvent(channel, EventType.CONNECTED, null);
	}

	@Override
	public void fireChannelInactive(Channel channel) throws IOException {
		System.out.println("EmbeddedChannelContextAware.fireChannelInactive():::");
		SocketAddress address = channel.getRemoteAddr();
		removeChannel(address);

		fireReconnectionIfNecessary(address);
		fireChannelEvent(channel, EventType.CLOSED, null);
	}

	@Override
	public void fireChannelReadable(Channel channel, MessagePacket packet) throws IOException {
		Object input = packet.getMessage();
		if (isPong(input)) {
			fireChannelEvent(channel, EventType.PONG, null);
		}
	}

	@Override
	public void fireChannelFatal(Channel channel, Throwable e) {
		e.printStackTrace();
		channel.close();

		SocketAddress address = channel.getRemoteAddr();
		removeChannel(address);

		fireReconnectionIfNecessary(address);
		fireChannelEvent(channel, EventType.FAULTY, e);
	}

	public void setChannelEventListener(ChannelEventListener<Channel> channelEventListener) {
		this.channelEventListener = channelEventListener;
	}

	public ChannelEventListener<Channel> getChannelEventListener() {
		return channelEventListener;
	}

	private void fireChannelEvent(Channel channel, EventType eventType, Throwable cause) {
		if (channelEventListener != null) {
			channelEventListener.fireChannelEvent(new ChannelEvent<Channel>(channel, eventType, cause));
		}
	}

	private void fireReconnectionIfNecessary(SocketAddress remoteAddress) {
		if (connectionWatcher != null) {
			connectionWatcher.reconnect(remoteAddress);
		}
	}

	private boolean isPong(Object data) {
		return (data instanceof Tuple) && ((Tuple) data).isPong();
	}

}
