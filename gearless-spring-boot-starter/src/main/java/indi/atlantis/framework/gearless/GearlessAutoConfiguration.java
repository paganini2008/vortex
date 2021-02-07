package indi.atlantis.framework.gearless;

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

import indi.atlantis.framework.gearless.buffer.BufferZone;
import indi.atlantis.framework.gearless.buffer.MemcachedBufferZone;
import indi.atlantis.framework.gearless.buffer.RedisBufferZone;
import indi.atlantis.framework.gearless.common.ChannelEventListener;
import indi.atlantis.framework.gearless.common.NamedSelectionPartitioner;
import indi.atlantis.framework.gearless.common.NioClient;
import indi.atlantis.framework.gearless.common.Partitioner;
import indi.atlantis.framework.gearless.common.embeddedio.EmbeddedClient;
import indi.atlantis.framework.gearless.common.embeddedio.EmbeddedSerializationFactory;
import indi.atlantis.framework.gearless.common.embeddedio.SerializationFactory;
import indi.atlantis.framework.gearless.common.grizzly.GrizzlyClient;
import indi.atlantis.framework.gearless.common.grizzly.GrizzlyTupleCodecFactory;
import indi.atlantis.framework.gearless.common.grizzly.TupleCodecFactory;
import indi.atlantis.framework.gearless.common.mina.MinaClient;
import indi.atlantis.framework.gearless.common.mina.MinaTupleCodecFactory;
import indi.atlantis.framework.gearless.common.netty.KeepAlivePolicy;
import indi.atlantis.framework.gearless.common.netty.MessageCodecFactory;
import indi.atlantis.framework.gearless.common.netty.NettyClient;
import indi.atlantis.framework.gearless.common.netty.NettyTupleCodecFactory;
import indi.atlantis.framework.gearless.common.serializer.FstSerializer;
import indi.atlantis.framework.gearless.common.serializer.Serializer;
import indi.atlantis.framework.gearless.transport.EmbeddedChannelEventListener;
import indi.atlantis.framework.gearless.transport.EmbeddedServer;
import indi.atlantis.framework.gearless.transport.EmbeddedServerHandler;
import indi.atlantis.framework.gearless.transport.GrizzlyChannelEventListener;
import indi.atlantis.framework.gearless.transport.GrizzlyServer;
import indi.atlantis.framework.gearless.transport.GrizzlyServerHandler;
import indi.atlantis.framework.gearless.transport.MinaChannelEventListener;
import indi.atlantis.framework.gearless.transport.MinaServer;
import indi.atlantis.framework.gearless.transport.MinaServerHandler;
import indi.atlantis.framework.gearless.transport.NettyChannelEventListener;
import indi.atlantis.framework.gearless.transport.NettyServer;
import indi.atlantis.framework.gearless.transport.NettyServerHandler;
import indi.atlantis.framework.gearless.transport.NettyServerKeepAlivePolicy;
import indi.atlantis.framework.gearless.transport.NioServer;
import indi.atlantis.framework.gearless.transport.NioServerStarter;
import indi.atlantis.framework.xmemcached.MemcachedTemplate;
import indi.atlantis.framework.xmemcached.MemcachedTemplateBuilder;
import indi.atlantis.framework.xmemcached.serializer.FstMemcachedSerializer;
import indi.atlantis.framework.xmemcached.serializer.MemcachedSerializer;
import io.netty.channel.Channel;

/**
 * 
 * GearlessAutoConfiguration
 * 
 * @author Jimmy Hoff
 * @version 1.0
 */
@Import({ ApplicationTransportController.class, BenchmarkController.class })
@Configuration
public class GearlessAutoConfiguration {

	@Value("${spring.application.cluster.name}")
	private String clusterName;

	@Bean
	public ApplicationTransportContext applicationTransportContext() {
		return new ApplicationTransportContext();
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

	@ConditionalOnMissingBean(name = "loopProcessorThreads")
	@Bean
	public ThreadPoolTaskExecutor loopProcessorTaskExecutor(
			@Value("${spring.application.cluster.transport.processor.threads:-1}") int taskExecutorThreads) {
		final int nThreads = taskExecutorThreads > 0 ? taskExecutorThreads : Runtime.getRuntime().availableProcessors() * 2;
		ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
		taskExecutor.setCorePoolSize(nThreads);
		taskExecutor.setMaxPoolSize(nThreads);
		taskExecutor.setThreadFactory(new PooledThreadFactory("spring-application-cluster-transport-executor-"));
		taskExecutor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
		return taskExecutor;
	}

	@ConditionalOnMissingBean
	@Bean
	public Partitioner partitioner() {
		return new NamedSelectionPartitioner();
	}

	@Bean
	public ProcessLogging processLogging() {
		return new ProcessLogging();
	}

	@Bean("consumer")
	public Counter consumer(RedisConnectionFactory redisConnectionFactory) {
		return new Counter(clusterName, "consumer", redisConnectionFactory);
	}

	@Bean("producer")
	public Counter producer(RedisConnectionFactory redisConnectionFactory) {
		return new Counter(clusterName, "producer", redisConnectionFactory);
	}

	@ConditionalOnMissingBean
	@Bean
	public BufferZone bufferZone() {
		return new RedisBufferZone();
	}

	/**
	 * 
	 * MemcachedBufferZoneConfiguration
	 * 
	 * @author Jimmy Hoff
	 *
	 * @since 1.0
	 */
	@Configuration
	@ConditionalOnProperty(name = "spring.application.cluster.transport.bufferzone", havingValue = "memcached")
	public static class MemcachedBufferZoneConfiguration {

		@Value("${spring.memcached.address:localhost:11211}")
		private String address;

		@ConditionalOnMissingBean(MemcachedTemplate.class)
		@Bean
		public MemcachedTemplate memcachedTemplate(MemcachedSerializer memcachedSerializer) throws Exception {
			MemcachedTemplateBuilder builder = new MemcachedTemplateBuilder();
			builder.setAddress(address);
			builder.setSerializer(new FstMemcachedSerializer());
			return builder.build();
		}

		@Bean
		public BufferZone bufferZone() {
			return new MemcachedBufferZone();
		}
	}

	@Configuration
	@ConditionalOnProperty(name = "spring.application.cluster.transport.nioserver", havingValue = "netty", matchIfMissing = true)
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
	@ConditionalOnProperty(name = "spring.application.cluster.transport.nioserver", havingValue = "mina")
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
	@ConditionalOnProperty(name = "spring.application.cluster.transport.nioserver", havingValue = "grizzly")
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
	@ConditionalOnProperty(name = "spring.application.cluster.transport.nioserver", havingValue = "embedded-io")
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
