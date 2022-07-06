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
package io.atlantisframework.vortex.transport;

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

import io.atlantisframework.vortex.common.TransportClientException;
import io.atlantisframework.vortex.common.grizzly.IdleTimeoutFilter;
import io.atlantisframework.vortex.common.grizzly.IdleTimeoutPolicies;
import io.atlantisframework.vortex.common.grizzly.TupleCodecFactory;
import io.atlantisframework.vortex.common.grizzly.TupleFilter;
import lombok.extern.slf4j.Slf4j;

/**
 * 
 * GrizzlyServer
 *
 * @author Fred Feng
 * @since 2.0.1
 */
@Slf4j
public class GrizzlyServer implements NioServer {

	private final AtomicBoolean started = new AtomicBoolean(false);
	private TCPNIOTransport transport;
	private DelayedExecutor delayedExecutor;
	private InetSocketAddress localAddress;

	@Value("${atlantis.framework.vortex.nioserver.threads:-1}")
	private int threadCount;

	@Value("${atlantis.framework.vortex.nioserver.hostName:}")
	private String hostName;

	@Value("${atlantis.framework.vortex.nioserver.idleTimeout:60}")
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
