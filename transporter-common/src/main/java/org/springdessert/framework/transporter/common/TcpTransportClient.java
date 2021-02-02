package org.springdessert.framework.transporter.common;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import org.springdessert.framework.transporter.common.netty.NettyClient;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.paganini2008.devtools.logging.Log;
import com.github.paganini2008.devtools.logging.LogFactory;
import com.github.paganini2008.devtools.multithreads.Executable;
import com.github.paganini2008.devtools.multithreads.ThreadUtils;
import com.github.paganini2008.devtools.net.UrlUtils;

/**
 * 
 * TcpTransportClient
 *
 * @author Jimmy Hoff
 * @version 1.0
 */
public class TcpTransportClient implements TransportClient, Executable {

	private static final Log logger = LogFactory.getLog(HttpTransportClient.class);
	private static final String servicePath = "/application/cluster/transport/tcp/services";

	public TcpTransportClient(String brokerUrl) {
		this(brokerUrl, url -> {
			return new NettyClient();
		});
	}

	public TcpTransportClient(String brokerUrl, Function<String, NioClient> supplier) {
		this.brokerUrl = brokerUrl;
		this.nioClient = supplier.apply(brokerUrl);
		ThreadUtils.scheduleWithFixedDelay(this, 5, TimeUnit.SECONDS);
	}

	private final ObjectMapper objectMapper = new ObjectMapper();
	private final String brokerUrl;
	private final NioClient nioClient;
	private Partitioner partitioner = new RoundRobinPartitioner();

	public void setPartitioner(Partitioner partitioner) {
		this.partitioner = partitioner;
	}

	@Override
	public void write(Tuple tuple) {
		if (isActive()) {
			nioClient.send(tuple, partitioner);
		}
	}

	@Override
	public boolean isActive() {
		return nioClient.isOpened();
	}

	@Override
	public void close() {
		nioClient.close();
	}

	@Override
	public boolean execute() {
		if (!nioClient.isOpened()) {
			nioClient.open();
		}
		String[] channels = getChannels();
		for (String channel : channels) {
			String[] args = channel.split(":", 2);
			try {
				InetSocketAddress socketAddress = new InetSocketAddress(args[0], Integer.parseInt(args[1]));
				if (!nioClient.isConnected(socketAddress)) {
					nioClient.connect(socketAddress, location -> {
						logger.info("TcpTransportClient connect to: " + location);
					});
				}
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			}
		}
		return isActive();
	}

	private String[] getChannels() {
		String content;
		try {
			content = UrlUtils.toString(brokerUrl + servicePath, "utf-8");
		} catch (IOException ignored) {
			logger.warn("");
			return new String[0];
		}
		try {
			return objectMapper.readValue(content, String[].class);
		} catch (IOException e) {
			throw new TransportClientException("Bad formatted content: " + content, e);
		}
	}

}
