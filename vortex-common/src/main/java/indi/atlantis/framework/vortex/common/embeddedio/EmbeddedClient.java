package indi.atlantis.framework.vortex.common.embeddedio;

import java.net.SocketAddress;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import com.github.paganini2008.devtools.multithreads.PooledThreadFactory;
import com.github.paganini2008.embeddedio.AioConnector;
import com.github.paganini2008.embeddedio.Channel;
import com.github.paganini2008.embeddedio.ChannelPromise;
import com.github.paganini2008.embeddedio.IdleChannelHandler;
import com.github.paganini2008.embeddedio.IdleTimeoutListener;
import com.github.paganini2008.embeddedio.IoConnector;
import com.github.paganini2008.embeddedio.NioConnector;
import com.github.paganini2008.embeddedio.SerializationTransformer;
import com.github.paganini2008.embeddedio.Transformer;

import indi.atlantis.framework.vortex.common.ConnectionWatcher;
import indi.atlantis.framework.vortex.common.HandshakeCallback;
import indi.atlantis.framework.vortex.common.NioClient;
import indi.atlantis.framework.vortex.common.Partitioner;
import indi.atlantis.framework.vortex.common.TransportClientException;
import indi.atlantis.framework.vortex.common.Tuple;

/**
 * 
 * EmbeddedClient
 *
 * @author Fred Feng
 * @since 1.0
 */
public class EmbeddedClient implements NioClient {

	private final AtomicBoolean opened = new AtomicBoolean(false);
	private final EmbeddedChannelContext channelContext = new EmbeddedChannelContext();
	private IoConnector connector;
	private int idleTimeout = 30;
	private int threadCount = -1;
	private SerializationFactory serializationFactory;

	@Override
	public void open() {
		final int nThreads = threadCount > 0 ? threadCount : Runtime.getRuntime().availableProcessors() * 2;
		Executor threadPool = Executors.newFixedThreadPool(nThreads, new PooledThreadFactory("transport-embedded-client-threads-"));
		connector = useAio ? new AioConnector(threadPool) : new NioConnector(threadPool);
		connector.setWriterBatchSize(100);
		connector.setWriterBufferSize(1024 * 1024);
		connector.setAutoFlushInterval(3);
		if (serializationFactory == null) {
			serializationFactory = new EmbeddedSerializationFactory();
		}
		Transformer transformer = new SerializationTransformer();
		transformer.setSerialization(serializationFactory.getEncoder(), serializationFactory.getDecoder());
		connector.setTransformer(transformer);
		if (idleTimeout > 0) {
			connector.addHandler(IdleChannelHandler.writerIdle(idleTimeout, 60, TimeUnit.SECONDS, new PingIdleTimeoutListener()));
		}
		connector.addHandler(channelContext);
		opened.set(true);
	}

	private boolean useAio = false;

	public void setUseAio(boolean useAio) {
		this.useAio = useAio;
	}

	@Override
	public void close() {
		try {
			channelContext.getChannels().forEach(channel -> {
				channel.close();
			});
		} catch (Exception e) {
			throw new TransportClientException(e.getMessage(), e);
		}
		if (connector != null) {
			connector.close();
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
			connector.connect(remoteAddress, new ChannelPromise<Channel>() {

				@Override
				public void onSuccess(Channel channel) {
					ConnectionWatcher connectionWatcher = channelContext.getConnectionWatcher();
					if (connectionWatcher != null) {
						connectionWatcher.watch(channel.getRemoteAddr(), handshakeCallback);
					}
					handshakeCallback.operationComplete(channel.getRemoteAddr());
				}

				@Override
				public void onFailure(Throwable e) {
					throw new TransportClientException(e.getMessage(), e);
				}
			});
		} catch (Exception e) {
			if (e instanceof TransportClientException) {
				throw (TransportClientException) e;
			}
			throw new TransportClientException(e.getMessage(), e);
		}
	}

	@Override
	public boolean isConnected(SocketAddress remoteAddress) {
		Channel channel = channelContext.getChannel(remoteAddress);
		return channel != null && channel.isActive();
	}

	@Override
	public void setThreadCount(int threadCount) {
		this.threadCount = threadCount;
	}

	@Override
	public void setIdleTimeout(int idleTimeout) {
		this.idleTimeout = idleTimeout;
	}

	@Override
	public void watchConnection(int checkInterval, TimeUnit timeUnit) {
		this.channelContext.setConnectionWatcher(new ConnectionWatcher(checkInterval, timeUnit, this));
	}

	public void setSerializationFactory(SerializationFactory serializationFactory) {
		this.serializationFactory = serializationFactory;
	}

	@Override
	public void send(Object data) {
		channelContext.getChannels().forEach(connection -> {
			doSend(connection, data);
		});
	}

	@Override
	public void send(SocketAddress address, Object data) {
		Channel channel = channelContext.getChannel(address);
		if (channel != null) {
			doSend(channel, data);
		}
	}

	@Override
	public void send(Object data, Partitioner partitioner) {
		Channel channel = channelContext.selectChannel(data, partitioner);
		if (channel != null) {
			doSend(channel, data);
		}
	}

	protected void doSend(Channel channel, Object data) {
		try {
			if (data instanceof CharSequence) {
				channel.writeAndFlush(Tuple.byString(((CharSequence) data).toString()));
			} else if (data instanceof Tuple) {
				channel.writeAndFlush(data);
			}
		} catch (Exception e) {
			throw new TransportClientException(e.getMessage(), e);
		}
	}

	/**
	 * 
	 * PingIdleTimeoutListener
	 *
	 * @author Fred Feng
	 * @since 1.0
	 */
	private static class PingIdleTimeoutListener implements IdleTimeoutListener {

		@Override
		public void handleIdleTimeout(Channel channel, long timeout) {
			channel.writeAndFlush(Tuple.PING);
		}
	}

}
