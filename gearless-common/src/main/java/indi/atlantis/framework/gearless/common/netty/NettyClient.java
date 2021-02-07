package indi.atlantis.framework.gearless.common.netty;

import java.net.SocketAddress;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import indi.atlantis.framework.gearless.common.ChannelEventListener;
import indi.atlantis.framework.gearless.common.ConnectionWatcher;
import indi.atlantis.framework.gearless.common.HandshakeCallback;
import indi.atlantis.framework.gearless.common.NioClient;
import indi.atlantis.framework.gearless.common.Partitioner;
import indi.atlantis.framework.gearless.common.TransportClientException;
import indi.atlantis.framework.gearless.common.Tuple;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.GenericFutureListener;

/**
 * 
 * NettyClient
 * 
 * @author Jimmy Hoff
 * @version 1.0
 */
public class NettyClient implements NioClient {

	private final NettyChannelContext channelContext = new NettyChannelContext();
	private final AtomicBoolean opened = new AtomicBoolean(false);
	private EventLoopGroup workerGroup;
	private Bootstrap bootstrap;
	private MessageCodecFactory messageCodecFactory;
	private int threadCount = -1;
	private int idleTimeout = 30;

	public void setThreadCount(int nThreads) {
		this.threadCount = nThreads;
	}

	public void setIdleTimeout(int idleTimeout) {
		this.idleTimeout = idleTimeout;
	}

	public void setMessageCodecFactory(MessageCodecFactory messageCodecFactory) {
		this.messageCodecFactory = messageCodecFactory;
	}

	public void watchConnection(int checkInterval, TimeUnit timeUnit) {
		this.channelContext.setConnectionWatcher(new ConnectionWatcher(checkInterval, timeUnit, this));
	}

	public void setChannelEventListener(ChannelEventListener<Channel> channelEventListener) {
		this.channelContext.setChannelEventListener(channelEventListener);
	}

	public void open() {
		int nThreads = threadCount > 0 ? threadCount : Runtime.getRuntime().availableProcessors() * 2;
		workerGroup = new NioEventLoopGroup(nThreads);
		bootstrap = new Bootstrap();
		bootstrap.group(workerGroup).channel(NioSocketChannel.class).option(ChannelOption.SO_KEEPALIVE, true)
				.option(ChannelOption.TCP_NODELAY, true).option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 60000)
				.option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT).option(ChannelOption.SO_SNDBUF, 1024 * 1024);
		if (messageCodecFactory == null) {
			messageCodecFactory = new NettyTupleCodecFactory();
		}
		bootstrap.handler(new ChannelInitializer<SocketChannel>() {
			public void initChannel(SocketChannel ch) throws Exception {
				ChannelPipeline pipeline = ch.pipeline();
				pipeline.addLast(new IdleStateHandler(0, idleTimeout, 0, TimeUnit.SECONDS));
				pipeline.addLast(new NettyClientKeepAlivePolicy());
				pipeline.addLast(messageCodecFactory.getEncoder(), messageCodecFactory.getDecoder());
				pipeline.addLast(channelContext);
			}
		});
		opened.set(true);
	}

	public boolean isOpened() {
		return opened.get();
	}

	public void connect(final SocketAddress remoteAddress, final HandshakeCallback handshakeCallback) {
		if (isConnected(remoteAddress)) {
			return;
		}
		try {
			bootstrap.connect(remoteAddress).addListener(new GenericFutureListener<ChannelFuture>() {
				public void operationComplete(ChannelFuture future) throws Exception {
					if (future.isSuccess()) {
						ConnectionWatcher connectionWatcher = channelContext.getConnectionWatcher();
						if (connectionWatcher != null) {
							connectionWatcher.watch(remoteAddress, handshakeCallback);
						}
						if (handshakeCallback != null) {
							handshakeCallback.operationComplete(remoteAddress);
						}
					}
				}
			}).sync();
		} catch (InterruptedException e) {
			throw new TransportClientException(e.getMessage(), e);
		}

	}

	public void send(Object data) {
		channelContext.getChannels().forEach(channel -> {
			doSend(channel, data);
		});
	}

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

	public void close() {
		try {
			channelContext.getChannels().forEach(channel -> {
				channel.close();
			});
		} catch (Exception e) {
			throw new TransportClientException(e.getMessage(), e);
		}
		try {
			workerGroup.shutdownGracefully();
		} catch (Exception e) {
			throw new TransportClientException(e.getMessage(), e);
		}
		opened.set(false);
	}

	public boolean isConnected(SocketAddress remoteAddress) {
		Channel channel = channelContext.getChannel(remoteAddress);
		return channel != null && channel.isActive();
	}

}
