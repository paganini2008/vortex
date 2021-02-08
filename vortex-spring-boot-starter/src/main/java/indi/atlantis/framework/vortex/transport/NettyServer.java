package indi.atlantis.framework.vortex.transport;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import com.github.paganini2008.devtools.StringUtils;
import com.github.paganini2008.devtools.net.NetUtils;

import indi.atlantis.framework.vortex.common.TransportClientException;
import indi.atlantis.framework.vortex.common.netty.KeepAlivePolicy;
import indi.atlantis.framework.vortex.common.netty.MessageCodecFactory;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;

/**
 * 
 * NettyServer
 * 
 * @author Jimmy Hoff
 * @version 1.0
 */
@Slf4j
public class NettyServer implements NioServer {

	private final AtomicBoolean started = new AtomicBoolean(false);
	private EventLoopGroup bossGroup;
	private EventLoopGroup workerGroup;

	@Autowired
	private NettyServerHandler serverHandler;

	@Autowired
	private MessageCodecFactory codecFactory;

	@Value("${atlantis.framework.vortex.nioserver.threads:-1}")
	private int threadCount;

	@Value("${atlantis.framework.vortex.nioserver.hostName:}")
	private String hostName;

	@Value("${atlantis.framework.vortex.nioserver.idleTimeout:60}")
	private int idleTimeout;

	@Autowired
	private KeepAlivePolicy keepAlivePolicy;

	public SocketAddress start() {
		if (isStarted()) {
			throw new IllegalStateException("Netty has been started.");
		}
		final int nThreads = threadCount > 0 ? threadCount : Runtime.getRuntime().availableProcessors() * 2;
		bossGroup = new NioEventLoopGroup(nThreads);
		workerGroup = new NioEventLoopGroup(nThreads);
		ServerBootstrap bootstrap = new ServerBootstrap();
		bootstrap.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class).option(ChannelOption.SO_BACKLOG, 128);
		bootstrap.childOption(ChannelOption.TCP_NODELAY, true).childOption(ChannelOption.SO_REUSEADDR, true)
				.childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
		bootstrap.childOption(ChannelOption.SO_RCVBUF, 2 * 1024 * 1024);
		bootstrap.childHandler(new ChannelInitializer<SocketChannel>() {
			public void initChannel(SocketChannel ch) throws Exception {
				ChannelPipeline pipeline = ch.pipeline();
				pipeline.addLast(new IdleStateHandler(idleTimeout, 0, 0, TimeUnit.SECONDS));
				pipeline.addLast(codecFactory.getEncoder(), codecFactory.getDecoder());
				pipeline.addLast(keepAlivePolicy);
				pipeline.addLast(serverHandler);
			}
		});
		int port = NetUtils.getRandomPort(PORT_RANGE_BEGIN, PORT_RANGE_END);
		InetSocketAddress socketAddress;
		try {
			socketAddress = StringUtils.isNotBlank(hostName) ? new InetSocketAddress(hostName, port) : new InetSocketAddress(port);
			bootstrap.bind(socketAddress).sync();
			started.set(true);
			log.info("Netty is started on: " + socketAddress);
		} catch (Exception e) {
			throw new TransportClientException(e.getMessage(), e);
		}
		return socketAddress;
	}

	public void stop() {
		if (!isStarted()) {
			return;
		}
		try {
			if (workerGroup != null) {
				workerGroup.shutdownGracefully();
			}
			if (bossGroup != null) {
				bossGroup.shutdownGracefully();
			}
			started.set(false);
			log.info("Netty is stoped successfully.");
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}

	public boolean isStarted() {
		return started.get();
	}

}
