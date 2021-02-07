package indi.atlantis.framework.gearless.transport;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.glassfish.grizzly.filterchain.FilterChainBuilder;
import org.glassfish.grizzly.filterchain.TransportFilter;
import org.glassfish.grizzly.nio.transport.TCPNIOTransport;
import org.glassfish.grizzly.nio.transport.TCPNIOTransportBuilder;
import org.glassfish.grizzly.strategies.WorkerThreadIOStrategy;
import org.glassfish.grizzly.threadpool.ThreadPoolConfig;
import org.glassfish.grizzly.utils.DelayedExecutor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import com.github.paganini2008.devtools.StringUtils;
import com.github.paganini2008.devtools.net.NetUtils;

import indi.atlantis.framework.gearless.common.TransportClientException;
import indi.atlantis.framework.gearless.common.grizzly.IdleTimeoutFilter;
import indi.atlantis.framework.gearless.common.grizzly.IdleTimeoutPolicies;
import indi.atlantis.framework.gearless.common.grizzly.TupleCodecFactory;
import indi.atlantis.framework.gearless.common.grizzly.TupleFilter;
import lombok.extern.slf4j.Slf4j;

/**
 * 
 * GrizzlyServer
 *
 * @author Jimmy Hoff
 * @version 1.0
 */
@Slf4j
public class GrizzlyServer implements NioServer {

	private final AtomicBoolean started = new AtomicBoolean(false);
	private TCPNIOTransport transport;
	private DelayedExecutor delayedExecutor;
	private InetSocketAddress localAddress;

	@Value("${spring.application.cluster.transport.nioserver.threads:-1}")
	private int threadCount;

	@Value("${spring.application.cluster.transport.nioserver.hostName:}")
	private String hostName;

	@Value("${spring.application.cluster.transport.nioserver.idleTimeout:60}")
	private int idleTimeout;

	@Autowired
	private GrizzlyServerHandler serverHandler;

	@Autowired
	private TupleCodecFactory codecFactory;

	@Override
	public SocketAddress start() {
		if (isStarted()) {
			throw new IllegalStateException("GrizzlyServer has been started.");
		}
		FilterChainBuilder filterChainBuilder = FilterChainBuilder.stateless();
		filterChainBuilder.add(new TransportFilter());
		delayedExecutor = IdleTimeoutFilter.createDefaultIdleDelayedExecutor(5, TimeUnit.SECONDS);
		delayedExecutor.start();
		IdleTimeoutFilter timeoutFilter = new IdleTimeoutFilter(delayedExecutor, idleTimeout, TimeUnit.SECONDS,
				IdleTimeoutPolicies.READER_IDLE_LOG);
		filterChainBuilder.add(timeoutFilter);
		filterChainBuilder.add(new TupleFilter(codecFactory));
		filterChainBuilder.add(serverHandler);
		TCPNIOTransportBuilder builder = TCPNIOTransportBuilder.newInstance();
		final int nThreads = threadCount > 0 ? threadCount : Runtime.getRuntime().availableProcessors() * 2;
		ThreadPoolConfig tpConfig = ThreadPoolConfig.defaultConfig();
		tpConfig.setPoolName("GrizzlyServerHandler").setQueueLimit(-1).setCorePoolSize(nThreads).setMaxPoolSize(nThreads)
				.setKeepAliveTime(60L, TimeUnit.SECONDS);
		builder.setWorkerThreadPoolConfig(tpConfig);
		builder.setKeepAlive(true).setReuseAddress(true).setReadBufferSize(2 * 1024 * 1024);
		builder.setIOStrategy(WorkerThreadIOStrategy.getInstance());
		builder.setServerConnectionBackLog(128);
		transport = builder.build();
		transport.setProcessor(filterChainBuilder.build());
		int port = NetUtils.getRandomPort(PORT_RANGE_BEGIN, PORT_RANGE_END);
		try {
			localAddress = StringUtils.isNotBlank(hostName) ? new InetSocketAddress(hostName, port) : new InetSocketAddress(port);
			transport.bind(localAddress);
			transport.start();
			started.set(true);
			log.info("Grizzly is started on: " + localAddress);
		} catch (Exception e) {
			throw new TransportClientException(e.getMessage(), e);
		}
		return localAddress;
	}

	@Override
	public void stop() {
		if (transport == null || !isStarted()) {
			return;
		}
		try {
			delayedExecutor.destroy();
			transport.shutdown(60, TimeUnit.SECONDS);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		started.set(false);
		log.info("Grizzly is closed successfully.");
	}

	@Override
	public boolean isStarted() {
		return started.get();
	}

}
