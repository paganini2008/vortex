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
package io.atlantisframework.vortex.common.mina;

import java.net.SocketAddress;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.buffer.SimpleBufferAllocator;
import org.apache.mina.core.future.IoFuture;
import org.apache.mina.core.future.IoFutureListener;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFactory;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.keepalive.KeepAliveFilter;
import org.apache.mina.filter.keepalive.KeepAliveMessageFactory;
import org.apache.mina.filter.keepalive.KeepAliveRequestTimeoutHandler;
import org.apache.mina.transport.socket.SocketSessionConfig;
import org.apache.mina.transport.socket.nio.NioSocketConnector;

import io.atlantisframework.vortex.common.ChannelEvent;
import io.atlantisframework.vortex.common.ChannelEventListener;
import io.atlantisframework.vortex.common.ConnectionWatcher;
import io.atlantisframework.vortex.common.HandshakeCallback;
import io.atlantisframework.vortex.common.NioClient;
import io.atlantisframework.vortex.common.Partitioner;
import io.atlantisframework.vortex.common.TransportClientException;
import io.atlantisframework.vortex.common.Tuple;
import io.atlantisframework.vortex.common.ChannelEvent.EventType;

/**
 * 
 * MinaClient
 * 
 * @author Fred Feng
 * @since 2.0.1
 */
public class MinaClient implements NioClient {

	static {
		IoBuffer.setUseDirectBuffer(false);
		IoBuffer.setAllocator(new SimpleBufferAllocator());
	}

	private final MinaChannelContext channelContext = new MinaChannelContext();
	private final AtomicBoolean opened = new AtomicBoolean(false);
	private ProtocolCodecFactory protocolCodecFactory;
	private NioSocketConnector connector;
	private int idleTimeout = 30;
	private int threadCount = -1;

	@Override
	public void setIdleTimeout(int idleTimeout) {
		this.idleTimeout = idleTimeout;
	}

	public void setProtocolCodecFactory(ProtocolCodecFactory protocolCodecFactory) {
		this.protocolCodecFactory = protocolCodecFactory;
	}

	@Override
	public void watchConnection(int checkInterval, TimeUnit timeUnit) {
		this.channelContext.setConnectionWatcher(new ConnectionWatcher(checkInterval, timeUnit, this));
	}

	@Override
	public void setThreadCount(int nThreads) {
		this.threadCount = nThreads;
	}

	public void setChannelEventListener(ChannelEventListener<IoSession> channelEventListener) {
		this.channelContext.setChannelEventListener(channelEventListener);
	}

	@Override
	public void open() {
		int nThreads = threadCount > 0 ? threadCount : Runtime.getRuntime().availableProcessors() * 2;
		connector = new NioSocketConnector(nThreads);
		connector.setConnectTimeoutMillis(60000);
		SocketSessionConfig sessionConfig = connector.getSessionConfig();
		sessionConfig.setKeepAlive(true);
		sessionConfig.setTcpNoDelay(true);
		sessionConfig.setSendBufferSize(1024 * 1024);
		if (protocolCodecFactory == null) {
			protocolCodecFactory = new MinaTupleCodecFactory();
		}

		KeepAliveFilter heartBeat = new KeepAliveFilter(new ClientKeepAliveMessageFactory(), IdleStatus.WRITER_IDLE);
		heartBeat.setForwardEvent(false);
		heartBeat.setRequestTimeout(idleTimeout);
		heartBeat.setRequestTimeoutHandler(KeepAliveRequestTimeoutHandler.LOG);
		connector.getFilterChain().addLast("codec", new ProtocolCodecFilter(protocolCodecFactory));
		connector.getFilterChain().addLast("heartbeat", heartBeat);
		connector.setHandler(channelContext);

		opened.set(true);
	}

	@Override
	public void close() {
		try {
			channelContext.getChannels().forEach(ioSession -> {
				ioSession.closeNow();
			});
		} catch (Exception e) {
			throw new TransportClientException(e.getMessage(), e);
		}
		try {
			if (connector != null) {
				connector.getFilterChain().clear();
				connector.dispose();
				connector = null;
			}
		} catch (Exception e) {
			throw new TransportClientException(e.getMessage(), e);
		}

		opened.set(false);
	}

	@Override
	public boolean isOpened() {
		return opened.get();
	}

	@Override
	public void connect(final SocketAddress remoteAddress, final HandshakeCallback handshakeCallback) {
		if (isConnected(remoteAddress)) {
			return;
		}
		try {
			connector.connect(remoteAddress).addListener(new IoFutureListener<IoFuture>() {
				public void operationComplete(IoFuture future) {
					if (future.isDone()) {
						ConnectionWatcher connectionWatcher = channelContext.getConnectionWatcher();
						if (connectionWatcher != null) {
							connectionWatcher.watch(remoteAddress, handshakeCallback);
						}
						if (handshakeCallback != null) {
							handshakeCallback.operationComplete(remoteAddress);
						}
					}
				}
			}).awaitUninterruptibly();
		} catch (Exception e) {
			throw new TransportClientException(e.getMessage(), e);
		}
	}

	@Override
	public boolean isConnected(SocketAddress remoteAddress) {
		IoSession ioSession = channelContext.getChannel(remoteAddress);
		return ioSession != null && ioSession.isConnected();
	}

	@Override
	public void send(Object data) {
		channelContext.getChannels().forEach(ioSession -> {
			doSend(ioSession, data);
		});
	}

	@Override
	public void send(SocketAddress address, Object data) {
		IoSession ioSession = channelContext.getChannel(address);
		if (ioSession != null) {
			doSend(ioSession, data);
		}
	}

	@Override
	public void send(Object data, Partitioner partitioner) {
		IoSession ioSession = channelContext.selectChannel(data, partitioner);
		if (ioSession != null) {
			doSend(ioSession, data);
		}
	}

	protected void doSend(IoSession ioSession, Object data) {
		try {
			if (data instanceof CharSequence) {
				ioSession.write(Tuple.byString(((CharSequence) data).toString()));
			} else if (data instanceof Tuple) {
				ioSession.write(data);
			}
		} catch (Exception e) {
			throw new TransportClientException(e.getMessage(), e);
		}
	}

	class ClientKeepAliveMessageFactory implements KeepAliveMessageFactory {

		public boolean isRequest(IoSession session, Object message) {
			return false;
		}

		public boolean isResponse(IoSession session, Object message) {
			return (message instanceof Tuple) && ((Tuple) message).isPong();
		}

		public Object getRequest(IoSession session) {
			return Tuple.PING;
		}

		public Object getResponse(IoSession session, Object request) {
			ChannelEventListener<IoSession> channelEventListener = channelContext.getChannelEventListener();
			if (channelEventListener != null) {
				channelEventListener.fireChannelEvent(new ChannelEvent<IoSession>(session, EventType.PONG, null));
			}
			return null;
		}
	}

}
