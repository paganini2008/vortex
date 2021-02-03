package org.springtribe.framework.gearless.transport;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springtribe.framework.gearless.common.TransportClientException;
import org.springtribe.framework.gearless.common.embeddedio.SerializationFactory;

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

import lombok.extern.slf4j.Slf4j;

/**
 * 
 * EmbeddedServer
 *
 * @author Jimmy Hoff
 * @since 1.0
 */
@Slf4j
public class EmbeddedServer implements NioServer {

	private final AtomicBoolean started = new AtomicBoolean(false);
	private IoAcceptor acceptor;
	private InetSocketAddress localAddress;

	@Value("${spring.application.cluster.transport.nioserver.threads:-1}")
	private int threadCount;

	@Value("${spring.application.cluster.transport.nioserver.hostName:}")
	private String hostName;

	@Value("${spring.application.cluster.transport.nioserver.idleTimeout:60}")
	private int idleTimeout;

	@Value("${spring.application.cluster.transport.nioserver.embeddedio.useAio:false}")
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
