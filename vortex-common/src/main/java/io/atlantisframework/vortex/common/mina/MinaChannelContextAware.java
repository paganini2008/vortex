/**
* Copyright 2017-2022 Fred Feng (paganini.fy@gmail.com)

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
package io.atlantisframework.vortex.common.mina;

import java.net.SocketAddress;

import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;

import io.atlantisframework.vortex.common.ChannelContext;
import io.atlantisframework.vortex.common.ChannelEvent;
import io.atlantisframework.vortex.common.ChannelEventListener;
import io.atlantisframework.vortex.common.ConnectionWatcher;
import io.atlantisframework.vortex.common.ChannelEvent.EventType;

/**
 * 
 * MinaChannelContextAware
 * 
 * @author Fred Feng
 * @since 2.0.1
 */
public abstract class MinaChannelContextAware extends IoHandlerAdapter implements ChannelContext<IoSession> {

	private ConnectionWatcher connectionWatcher;
	private ChannelEventListener<IoSession> channelEventListener;

	public ConnectionWatcher getConnectionWatcher() {
		return connectionWatcher;
	}

	public void setConnectionWatcher(ConnectionWatcher connectionWatcher) {
		this.connectionWatcher = connectionWatcher;
	}

	@Override
	public void sessionOpened(IoSession session) throws Exception {
		addChannel(session);

		fireChannelEvent(session, EventType.CONNECTED, null);
	}

	@Override
	public void sessionClosed(IoSession session) throws Exception {
		removeChannel(session.getRemoteAddress());

		fireReconnectionIfNecessary(session.getRemoteAddress());
		fireChannelEvent(session, EventType.CLOSED, null);
	}

	@Override
	public void exceptionCaught(IoSession session, Throwable cause) throws Exception {
		session.closeNow();

		SocketAddress remoteAddress = session.getRemoteAddress();
		removeChannel(remoteAddress);

		fireReconnectionIfNecessary(session.getRemoteAddress());
		fireChannelEvent(session, EventType.FAULTY, cause);
	}

	public void setChannelEventListener(ChannelEventListener<IoSession> channelEventListener) {
		this.channelEventListener = channelEventListener;
	}

	public ChannelEventListener<IoSession> getChannelEventListener() {
		return channelEventListener;
	}

	private void fireChannelEvent(IoSession channel, EventType eventType, Throwable cause) {
		if (channelEventListener != null) {
			channelEventListener.fireChannelEvent(new ChannelEvent<IoSession>(channel, eventType, cause));
		}
	}

	private void fireReconnectionIfNecessary(SocketAddress remoteAddress) {
		if (connectionWatcher != null) {
			connectionWatcher.reconnect(remoteAddress);
		}
	}

}
