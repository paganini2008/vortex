/**
* Copyright 2018-2021 Fred Feng (paganini.fy@gmail.com)

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
package indi.atlantis.framework.vortex.common;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.paganini2008.devtools.logging.Log;
import com.github.paganini2008.devtools.logging.LogFactory;
import com.github.paganini2008.devtools.multithreads.Executable;
import com.github.paganini2008.devtools.multithreads.ThreadUtils;
import com.github.paganini2008.devtools.net.Urls;

import indi.atlantis.framework.vortex.common.netty.NettyClient;

/**
 * 
 * TcpTransportClient
 *
 * @author Fred Feng
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
			content = Urls.toString(brokerUrl + servicePath, "utf-8");
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
