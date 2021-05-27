package indi.atlantis.framework.vortex.transport;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import com.github.paganini2008.embeddedio.Channel;
import com.github.paganini2008.embeddedio.ChannelHandler;
import com.github.paganini2008.embeddedio.MessagePacket;

import indi.atlantis.framework.vortex.Accumulator;
import indi.atlantis.framework.vortex.buffer.BufferZone;
import indi.atlantis.framework.vortex.common.ChannelEvent;
import indi.atlantis.framework.vortex.common.ChannelEvent.EventType;
import indi.atlantis.framework.vortex.common.ChannelEventListener;
import indi.atlantis.framework.vortex.common.Tuple;
import lombok.extern.slf4j.Slf4j;

/**
 * 
 * EmbeddedServerHandler
 *
 * @author Fred Feng
 * @since 1.0
 */
@Slf4j
public class EmbeddedServerHandler implements ChannelHandler {

	@Autowired
	private BufferZone store;

	@Autowired
	private Accumulator accumulator;

	@Value("${atlantis.framework.vortex.bufferzone.collectionName}")
	private String collectionName;

	@Value("${atlantis.framework.vortex.nioserver.keepalive.response:true}")
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
				store.set(collectionName, (Tuple) message);
				accumulator.accumulate((Tuple) message);
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
