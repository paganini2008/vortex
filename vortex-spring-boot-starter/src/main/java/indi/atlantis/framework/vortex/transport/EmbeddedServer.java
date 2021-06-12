/**
* Copyright 2021 Fred Feng (paganini.fy@gmail.com)

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
package indi.atlantis.framework.vortex.transport;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import com.github.paganini2008.devtools.StringUtils;
import com.github.paganini2008.devtools.multithreads.PooledThreadFactory;
import com.github.paganini2008.devtools.net.NetUtils;
import com.github.paganini2008.embeddedio.AioAcceptor;
import com.github.paganini2008.embeddedio.IdleChannelHandler;
import com.github.paganini2008.embeddedio.IdleTimeoutListener;
import com.github.paganini2008.embeddedio.IoAcceptor;
import com.github.paganini2008.embeddedio.NioAcceptor;
import com.github.paganini2008.embeddedio.SerializationTransformer;
import com.github.paganini2008.embeddedio.Transformer;

import indi.atlantis.framework.vortex.common.TransportClientException;
import indi.atlantis.framework.vortex.common.embeddedio.SerializationFactory;
import lombok.extern.slf4j.Slf4j;

/**
 * 
 * EmbeddedServer
 *
 * @author Fred Feng
 * @since 1.0
 */
@Slf4j
public class EmbeddedServer implements NioServer {

	private final AtomicBoolean started = new AtomicBoolean(false);
	private IoAcceptor acceptor;
	private InetSocketAddress localAddress;

	@Value("${atlantis.framework.vortex.nioserver.threads:-1}")
	private int threadCount;

	@Value("${atlantis.framework.vortex.nioserver.hostName:}")
	private String hostName;

	@Value("${atlantis.framework.vortex.nioserver.idleTimeout:60}")
	private int idleTimeout;

	@Value("${atlantis.framework.vortex.nioserver.embeddedio.useAio:false}")
	private boolean useAio;

	@Autowired
	private EmbeddedServerHandler serverHandler;

	@Autowired
	private SerializationFactory serializationFactory;

	@Override
	public SocketAddress start() throws Exception {
		if (isStarted()) {
			throw new IllegalStateException("EmbeddedServer has been started.");
		}
		final int nThreads = threadCount > 0 ? threadCount : Runtime.getRuntime().availableProcessors() * 2;
		ExecutorService executor = Executors.newFixedThreadPool(nThreads, new PooledThreadFactory("transport-embedded-server-threads-"));
		acceptor = useAio ? new AioAcceptor(executor) : new NioAcceptor(executor);
		acceptor.setBacklog(128);
		acceptor.setReaderBufferSize(2 * 1024 * 1024);
		Transformer transformer = new SerializationTransformer();
		transformer.setSerialization(serializationFactory.getEncoder(), serializationFactory.getDecoder());
		acceptor.setTransformer(transformer);
		if (idleTimeout > 0) {
			acceptor.addHandler(IdleChannelHandler.readerIdle(idleTimeout, 60, TimeUnit.SECONDS, IdleTimeoutListener.LOG));
		}
		acceptor.addHandler(serverHandler);

		int port = NetUtils.getRandomPort(PORT_RANGE_BEGIN, PORT_RANGE_END);
		localAddress = StringUtils.isNotBlank(hostName) ? new InetSocketAddress(hostName, port) : new InetSocketAddress(port);
		acceptor.setLocalAddress(localAddress);
		try {
			acceptor.start();
			started.set(true);
			log.info("EmbeddedServer is started on: " + localAddress);
		} catch (IOException e) {
			throw new TransportClientException(e.getMessage(), e);
		}
		return localAddress;
	}

	@Override
	public void stop() {
		if (acceptor == null || !isStarted()) {
			return;
		}
		acceptor.stop();
		started.set(false);
		log.info("EmbeddedServer is shutdown.");
	}

	@Override
	public boolean isStarted() {
		return started.get();
	}

}
