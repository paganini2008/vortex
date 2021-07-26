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
package indi.atlantis.framework.vortex;

import java.util.concurrent.ThreadPoolExecutor;

import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFactory;
import org.glassfish.grizzly.Connection;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import com.github.paganini2008.devtools.multithreads.PooledThreadFactory;
import com.github.paganini2008.springdessert.reditools.common.RedisCalulation;

import indi.atlantis.framework.vortex.buffer.BufferZone;
import indi.atlantis.framework.vortex.buffer.RedisBufferZone;
import indi.atlantis.framework.vortex.common.ChannelEventListener;
import indi.atlantis.framework.vortex.common.NamedSelectionPartitioner;
import indi.atlantis.framework.vortex.common.NioClient;
import indi.atlantis.framework.vortex.common.Partitioner;
import indi.atlantis.framework.vortex.common.embeddedio.EmbeddedClient;
import indi.atlantis.framework.vortex.common.embeddedio.EmbeddedSerializationFactory;
import indi.atlantis.framework.vortex.common.embeddedio.SerializationFactory;
import indi.atlantis.framework.vortex.common.grizzly.GrizzlyClient;
import indi.atlantis.framework.vortex.common.grizzly.GrizzlyTupleCodecFactory;
import indi.atlantis.framework.vortex.common.grizzly.TupleCodecFactory;
import indi.atlantis.framework.vortex.common.mina.MinaClient;
import indi.atlantis.framework.vortex.common.mina.MinaTupleCodecFactory;
import indi.atlantis.framework.vortex.common.netty.KeepAlivePolicy;
import indi.atlantis.framework.vortex.common.netty.MessageCodecFactory;
import indi.atlantis.framework.vortex.common.netty.NettyClient;
import indi.atlantis.framework.vortex.common.netty.NettyTupleCodecFactory;
import indi.atlantis.framework.vortex.common.serializer.FstSerializer;
import indi.atlantis.framework.vortex.common.serializer.Serializer;
import indi.atlantis.framework.vortex.transport.EmbeddedChannelEventListener;
import indi.atlantis.framework.vortex.transport.EmbeddedServer;
import indi.atlantis.framework.vortex.transport.EmbeddedServerHandler;
import indi.atlantis.framework.vortex.transport.GrizzlyChannelEventListener;
import indi.atlantis.framework.vortex.transport.GrizzlyServer;
import indi.atlantis.framework.vortex.transport.GrizzlyServerHandler;
import indi.atlantis.framework.vortex.transport.MinaChannelEventListener;
import indi.atlantis.framework.vortex.transport.MinaServer;
import indi.atlantis.framework.vortex.transport.MinaServerHandler;
import indi.atlantis.framework.vortex.transport.NettyChannelEventListener;
import indi.atlantis.framework.vortex.transport.NettyServer;
import indi.atlantis.framework.vortex.transport.NettyServerHandler;
import indi.atlantis.framework.vortex.transport.NettyServerKeepAlivePolicy;
import indi.atlantis.framework.vortex.transport.NioServer;
import indi.atlantis.framework.vortex.transport.NioServerStarter;
import io.netty.channel.Channel;

/**
 * 
 * NioTransportAutoConfiguration
 * 
 * @author Fred Feng
 * @since 2.0.1
 */
@Import({ NioTransportController.class, BenchmarkController.class })
@Configuration(proxyBeanMethods = false)
public class NioTransportAutoConfiguration {

	@Value("${spring.application.cluster.name}")
	private String clusterName;

	@Bean
	public NioTransportContext nioTransportContext() {
		return new NioTransportContext();
	}

	@Bean
	public NioServerStarter nioServerStarter() {
		return new NioServerStarter();
	}

	@ConditionalOnMissingBean
	@Bean
	public Serializer serializer() {
		return new FstSerializer();
	}

	@Bean
	public TupleLoopProcessor tupleLoopProcessor() {
		return new TupleLoopProcessor();
	}

	@ConditionalOnMissingBean(name = "loopProcessorThreadPool")
	@Bean(destroyMethod = "shutdown")
	public ThreadPoolTaskExecutor loopProcessorThreadPool(@Value("${atlantis.framework.vortex.processor.threads:8}") int nThreads) {
		ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
		taskExecutor.setCorePoolSize(nThreads);
		taskExecutor.setMaxPoolSize(nThreads);
		taskExecutor.setThreadFactory(new PooledThreadFactory("atlantis-vortex-loop-processor-threads"));
		taskExecutor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
		return taskExecutor;
	}

	@Bean
	public Partitioner partitioner() {
		return new NamedSelectionPartitioner();
	}

	@Bean
	public ProcessLogging processLogging() {
		return new ProcessLogging();
	}

	@Bean
	public Accumulator accumulator(RedisConnectionFactory redisConnectionFactory) {
		return new Accumulator(clusterName, new RedisCalulation(redisConnectionFactory));
	}

	@Bean
	public NioTransportHealthIndicator nioTransportHealthIndicator() {
		return new NioTransportHealthIndicator();
	}

	@ConditionalOnMissingBean
	@Bean
	public BufferZone redisBufferZone(RedisConnectionFactory redisConnectionFactory) {
		RedisBufferZone bufferZone = new RedisBufferZone(redisConnectionFactory);
		bufferZone.setCollectionNamePrefix(BufferZone.DEFAULT_COLLECTION_NAME_PREFIX, clusterName);
		return bufferZone;
	}

	@Configuration
	@ConditionalOnProperty(name = "atlantis.framework.vortex.nioserver", havingValue = "netty", matchIfMissing = true)
	public static class NettyTransportConfiguration {

		@Bean(initMethod = "open", destroyMethod = "close")
		public NioClient nioClient(MessageCodecFactory codecFactory) {
			NettyClient nioClient = new NettyClient();
			nioClient.setMessageCodecFactory(codecFactory);
			return nioClient;
		}

		@Bean
		public NioServer nioServer() {
			return new NettyServer();
		}

		@ConditionalOnMissingBean(KeepAlivePolicy.class)
		@Bean
		public KeepAlivePolicy idlePolicy() {
			return new NettyServerKeepAlivePolicy();
		}

		@ConditionalOnMissingBean(MessageCodecFactory.class)
		@Bean
		public MessageCodecFactory codecFactory(Serializer serializer) {
			return new NettyTupleCodecFactory(serializer);
		}

		@Bean
		public NettyServerHandler serverHandler() {
			return new NettyServerHandler();
		}

		@ConditionalOnMissingBean(ChannelEventListener.class)
		@Bean
		public ChannelEventListener<Channel> channelEventListener() {
			return new NettyChannelEventListener();
		}
	}

	@Configuration
	@ConditionalOnProperty(name = "atlantis.framework.vortex.nioserver", havingValue = "mina")
	public static class MinaTransportConfiguration {

		@Bean(initMethod = "open", destroyMethod = "close")
		public NioClient nioClient(ProtocolCodecFactory codecFactory) {
			MinaClient nioClient = new MinaClient();
			nioClient.setProtocolCodecFactory(codecFactory);
			return nioClient;
		}

		@Bean
		public NioServer nioServer() {
			return new MinaServer();
		}

		@ConditionalOnMissingBean(ProtocolCodecFactory.class)
		@Bean
		public ProtocolCodecFactory codecFactory(Serializer serializer) {
			return new MinaTupleCodecFactory(serializer);
		}

		@Bean
		public MinaServerHandler serverHandler() {
			return new MinaServerHandler();
		}

		@ConditionalOnMissingBean(ChannelEventListener.class)
		@Bean
		public ChannelEventListener<IoSession> channelEventListener() {
			return new MinaChannelEventListener();
		}
	}

	@Configuration
	@ConditionalOnProperty(name = "atlantis.framework.vortex.nioserver", havingValue = "grizzly")
	public static class GrizzlyTransportConfiguration {

		@Bean(initMethod = "open", destroyMethod = "close")
		public NioClient nioClient(TupleCodecFactory codecFactory) {
			GrizzlyClient nioClient = new GrizzlyClient();
			nioClient.setTupleCodecFactory(codecFactory);
			return nioClient;
		}

		@Bean
		public NioServer nioServer() {
			return new GrizzlyServer();
		}

		@ConditionalOnMissingBean(TupleCodecFactory.class)
		@Bean
		public TupleCodecFactory codecFactory(Serializer serializer) {
			return new GrizzlyTupleCodecFactory(serializer);
		}

		@Bean
		public GrizzlyServerHandler serverHandler() {
			return new GrizzlyServerHandler();
		}

		@ConditionalOnMissingBean(ChannelEventListener.class)
		@Bean
		public ChannelEventListener<Connection<?>> channelEventListener() {
			return new GrizzlyChannelEventListener();
		}
	}

	@Configuration
	@ConditionalOnProperty(name = "atlantis.framework.vortex.nioserver", havingValue = "embedded-io")
	public static class EmbeddedIOTransportConfiguration {

		@Bean(initMethod = "open", destroyMethod = "close")
		public NioClient nioClient(SerializationFactory serializationFactory) {
			EmbeddedClient nioClient = new EmbeddedClient();
			nioClient.setSerializationFactory(serializationFactory);
			return nioClient;
		}

		@Bean
		public NioServer nioServer() {
			return new EmbeddedServer();
		}

		@ConditionalOnMissingBean(SerializationFactory.class)
		@Bean
		public SerializationFactory serializationFactory(Serializer serializer) {
			return new EmbeddedSerializationFactory(serializer);
		}

		@Bean
		public EmbeddedServerHandler serverHandler() {
			return new EmbeddedServerHandler();
		}

		@ConditionalOnMissingBean(ChannelEventListener.class)
		@Bean
		public ChannelEventListener<com.github.paganini2008.embeddedio.Channel> channelEventListener() {
			return new EmbeddedChannelEventListener();
		}
	}

}
