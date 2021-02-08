package indi.atlantis.framework.vortex.common.grizzly;

import java.io.IOException;
import java.net.SocketAddress;

import org.glassfish.grizzly.Connection;
import org.glassfish.grizzly.filterchain.BaseFilter;
import org.glassfish.grizzly.filterchain.FilterChainContext;
import org.glassfish.grizzly.filterchain.NextAction;

import indi.atlantis.framework.vortex.common.ChannelContext;
import indi.atlantis.framework.vortex.common.ChannelEvent;
import indi.atlantis.framework.vortex.common.ChannelEventListener;
import indi.atlantis.framework.vortex.common.ConnectionWatcher;
import indi.atlantis.framework.vortex.common.Tuple;
import indi.atlantis.framework.vortex.common.ChannelEvent.EventType;

/**
 * 
 * GrizzlyChannelContextAware
 *
 * @author Jimmy Hoff
 * @version 1.0
 */
public abstract class GrizzlyChannelContextAware extends BaseFilter implements ChannelContext<Connection<?>> {

	private ConnectionWatcher connectionWatcher;
	private ChannelEventListener<Connection<?>> channelEventListener;

	public ConnectionWatcher getConnectionWatcher() {
		return connectionWatcher;
	}

	public void setConnectionWatcher(ConnectionWatcher connectionWatcher) {
		this.connectionWatcher = connectionWatcher;
	}

	@Override
	public NextAction handleConnect(FilterChainContext ctx) throws IOException {
		addChannel(ctx.getConnection());

		fireChannelEvent(ctx.getConnection(), EventType.CONNECTED, null);
		return ctx.getInvokeAction();
	}

	@Override
	public NextAction handleClose(FilterChainContext ctx) throws IOException {
		SocketAddress address = (SocketAddress) ctx.getConnection().getPeerAddress();
		removeChannel(address);

		fireReconnectionIfNecessary(address);
		fireChannelEvent(ctx.getConnection(), EventType.CLOSED, null);
		return ctx.getInvokeAction();
	}

	@Override
	public NextAction handleRead(FilterChainContext ctx) throws IOException {
		Tuple input = ctx.getMessage();
		if (isPong(input)) {
			fireChannelEvent(ctx.getConnection(), EventType.PONG, null);
			return ctx.getStopAction();
		}
		return ctx.getInvokeAction();
	}

	@Override
	public void exceptionOccurred(FilterChainContext ctx, Throwable error) {
		ctx.getConnection().close();

		SocketAddress address = (SocketAddress) ctx.getConnection().getPeerAddress();
		removeChannel(address);

		fireReconnectionIfNecessary(address);
		fireChannelEvent(ctx.getConnection(), EventType.FAULTY, error);
	}

	public void setChannelEventListener(ChannelEventListener<Connection<?>> channelEventListener) {
		this.channelEventListener = channelEventListener;
	}

	public ChannelEventListener<Connection<?>> getChannelEventListener() {
		return channelEventListener;
	}

	private void fireChannelEvent(Connection<?> channel, EventType eventType, Throwable cause) {
		if (channelEventListener != null) {
			channelEventListener.fireChannelEvent(new ChannelEvent<Connection<?>>(channel, eventType, cause));
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
