/**
* Copyright 2017-2021 Fred Feng (paganini.fy@gmail.com)

* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
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
 * @author Fred Feng
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
