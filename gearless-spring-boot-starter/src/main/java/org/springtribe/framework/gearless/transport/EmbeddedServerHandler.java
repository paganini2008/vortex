package org.springtribe.framework.gearless.transport;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springtribe.framework.gearless.Counter;
import org.springtribe.framework.gearless.buffer.BufferZone;
import org.springtribe.framework.gearless.common.ChannelEvent;
import org.springtribe.framework.gearless.common.ChannelEventListener;
import org.springtribe.framework.gearless.common.Tuple;
import org.springtribe.framework.gearless.common.ChannelEvent.EventType;

import com.github.paganini2008.embeddedio.Channel;
import com.github.paganini2008.embeddedio.ChannelHandler;
import com.github.paganini2008.embeddedio.MessagePacket;

import lombok.extern.slf4j.Slf4j;

/**
 * 
 * EmbeddedServerHandler
 *
 * @author Jimmy Hoff
 * @since 1.0
 */
@Slf4j
public class EmbeddedServerHandler implements ChannelHandler {

	@Autowired
	private BufferZone store;

	@Qualifier("producer")
	@Autowired
	private Counter counter;

	@Value("${spring.application.cluster.transport.bufferzone.collectionName}")
	private String collectionName;

	@Value("${spring.application.cluster.transport.nioserver.keepalive.response:true}")
	private boolean keepaliveResposne;

	@Autowired(required = false)
	private ChannelEventListener<Channel> channelEventListener;

	@Override
	public void fireChannelActive(Channel channel) throws IOException {
		fireChannelEvent(channel, EventType.CONNECTED, null);
	}

	@Override
	public void fireChannelInactive(Channel channel) throws IOException {
		fireChannelEvent(channel, EventType.CLOSED, null);
	}

	@Override
	public void fireChannelReadable(Channel channel, MessagePacket packet) throws Exception {
		if (isPing(packet.getMessage())) {
			if (channelEventListener != null) {
				channelEventListener.fireChannelEvent(new ChannelEvent<Channel>(channel, EventType.PING, null));
			}
			if (keepaliveResposne) {
				channel.writeAndFlush(Tuple.PONG);
			}
		} else {
			for (Object message : packet.getMessages()) {
				counter.incrementCount();
				store.set(collectionName, (Tuple) message);
			}
		}
	}

	@Override
	public void fireChannelFatal(Channel channel, Throwable e) {
		log.error(e.getMessage(), e);
		fireChannelEvent(channel, EventType.FAULTY, e);
	}

	protected boolean isPing(Object data) {
		return (data instanceof Tuple) && ((Tuple) data).isPing();
	}

	private void fireChannelEvent(Channel channel, EventType eventType, Throwable cause) {
		if (channelEventListener != null) {
			channelEventListener.fireChannelEvent(new ChannelEvent<Channel>(channel, eventType, cause));
		}
	}

}
