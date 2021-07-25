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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.glassfish.grizzly.CompletionHandler;
import org.glassfish.grizzly.Connection;
import org.glassfish.grizzly.filterchain.FilterChainBuilder;
import org.glassfish.grizzly.filterchain.TransportFilter;
import org.glassfish.grizzly.nio.transport.TCPNIOTransport;
import org.glassfish.grizzly.nio.transport.TCPNIOTransportBuilder;
import org.glassfish.grizzly.strategies.WorkerThreadIOStrategy;
import org.glassfish.grizzly.threadpool.ThreadPoolConfig;
import org.glassfish.grizzly.utils.DelayedExecutor;

import indi.atlantis.framework.vortex.common.ConnectionWatcher;
import indi.atlantis.framework.vortex.common.HandshakeCallback;
import indi.atlantis.framework.vortex.common.NioClient;
import indi.atlantis.framework.vortex.common.Partitioner;
import indi.atlantis.framework.vortex.common.TransportClientException;
import indi.atlantis.framework.vortex.common.Tuple;

/**
 * 
 * GrizzlyClient
 *
 * @author Fred Feng
 * @version 1.0
 */
public class GrizzlyClient implements NioClient {

	private final AtomicBoolean opened = new AtomicBoolean(false);
	private final GrizzlyChannelContext channelContext = new GrizzlyChannelContext();
	private int idleTimeout = 30;
	private int threadCount = -1;
	private TCPNIOTransport transport;
	private DelayedExecutor delayedExecutor;
	private TupleCodecFactory codecFactory;

	@Override
	public void open() {
		FilterChainBuilder filterChainBuilder = FilterChainBuilder.stateless();
		filterChainBuilder.add(new TransportFilter());
		delayedExecutor = IdleTimeoutFilter.createDefaultIdleDelayedExecutor(5, TimeUnit.SECONDS);
		delayedExecutor.start();
		IdleTimeoutFilter timeoutFilter = new IdleTimeoutFilter(delayedExecutor, idleTimeout, TimeUnit.SECONDS, IdleTimeoutPolicies.PING);
		filterChainBuilder.add(timeoutFilter);
		if (codecFactory == null) {
			codecFactory = new GrizzlyTupleCodecFactory();
		}
		filterChainBuilder.add(new TupleFilter(codecFactory));
		filterChainBuilder.add(channelContext);
		TCPNIOTransportBuilder builder = TCPNIOTransportBuilder.newInstance();
		ThreadPoolConfig tpConfig = ThreadPoolConfig.defaultConfig();
		int nThreads = threadCount > 0 ? threadCount : Runtime.getRuntime().availableProcessors() * 2;
		tpConfig.setPoolName("GrizzlyClientHandler").setQueueLimit(-1).setCorePoolSize(nThreads).setMaxPoolSize(nThreads)
				.setKeepAliveTime(60L, TimeUnit.SECONDS);
		builder.setIOStrategy(WorkerThreadIOStrategy.getInstance());
		builder.setWorkerThreadPoolConfig(tpConfig);
		builder.setKeepAlive(true).setTcpNoDelay(true).setConnectionTimeout(60000).setWriteBufferSize(1024 * 1024);
		transport = builder.build();
		transport.setProcessor(filterChainBuilder.build());
		try {
			transport.start();
		} catch (IOException e) {
			throw new TransportClientException(e.getMessage(), e);
		}
		opened.set(true);
	}

	@Override
	public void close() {
		try {
			channelContext.getChannels().forEach(connection -> {
				connection.close();
			});
		} catch (Exception e) {
			throw new TransportClientException(e.getMessage(), e);
		}
		try {
			delayedExecutor.destroy();
			transport.shutdown(60, TimeUnit.SECONDS);
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
	public void connect(SocketAddress remoteAddress, HandshakeCallback handshakeCallback) {
		if (isConnected(remoteAddress)) {
			return;
		}
		try {
			transport.connect(remoteAddress, new DefaultCompletionHandler(handshakeCallback));
		} catch (Exception e) {
			throw new TransportClientException(e.getMessage(), e);
		}
	}

	@SuppressWarnings("rawtypes")
	class DefaultCompletionHandler implements CompletionHandler<Connection> {

		private final HandshakeCallback handshakeCallback;

		DefaultCompletionHandler(HandshakeCallback handshakeCallback) {
			this.handshakeCallback = handshakeCallback;
		}

		public void cancelled() {
			throw new TransportClientException("Connection is cancelled.");
		}

		public void failed(Throwable e) {
			throw new TransportClientException(e.getMessage(), e);
		}

		public void updated(Connection connection) {
		}

		public void completed(Connection connection) {
			if (connection.isOpen()) {
				SocketAddress remoteAddress = (SocketAddress) connection.getPeerAddress();
				ConnectionWatcher connectionWatcher = channelContext.getConnectionWatcher();
				if (connectionWatcher != null) {
					connectionWatcher.watch(remoteAddress, handshakeCallback);
				}
				if (handshakeCallback != null) {
					handshakeCallback.operationComplete(remoteAddress);
				}
			}
		}

	}

	@Override
	public boolean isConnected(SocketAddress remoteAddress) {
		Connection<?> connection = channelContext.getChannel(remoteAddress);
		return connection != null && connection.isOpen();
	}

	public void setTupleCodecFactory(TupleCodecFactory codecFactory) {
		this.codecFactory = codecFactory;
	}

	@Override
	public void setThreadCount(int threadCount) {
		this.threadCount = threadCount;
	}

	@Override
	public void watchConnection(int checkInterval, TimeUnit timeUnit) {
		this.channelContext.setConnectionWatcher(new ConnectionWatcher(checkInterval, timeUnit, this));
	}

	@Override
	public void setIdleTimeout(int idleTimeout) {
		this.idleTimeout = idleTimeout;
	}

	@Override
	public void send(Object data) {
		channelContext.getChannels().forEach(connection -> {
			doSend(connection, data);
		});
	}

	@Override
	public void send(SocketAddress address, Object data) {
		Connection<?> connection = channelContext.getChannel(address);
		if (connection != null) {
			doSend(connection, data);
		}
	}

	@Override
	public void send(Object data, Partitioner partitioner) {
		Connection<?> connection = channelContext.selectChannel(data, partitioner);
		if (connection != null) {
			doSend(connection, data);
		}
	}

	protected void doSend(Connection<?> connection, Object data) {
		try {
			if (data instanceof CharSequence) {
				connection.write(Tuple.byString(((CharSequence) data).toString()));
			} else if (data instanceof Tuple) {
				connection.write(data);
			}
		} catch (Exception e) {
			throw new TransportClientException(e.getMessage(), e);
		}
	}

}
