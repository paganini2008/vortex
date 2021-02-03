package org.springtribe.framework.gearless.transport;

import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springtribe.framework.gearless.Counter;
import org.springtribe.framework.gearless.buffer.BufferZone;
import org.springtribe.framework.gearless.common.ChannelEvent;
import org.springtribe.framework.gearless.common.ChannelEventListener;
import org.springtribe.framework.gearless.common.Tuple;
import org.springtribe.framework.gearless.common.ChannelEvent.EventType;

import lombok.extern.slf4j.Slf4j;

/**
 * 
 * MinaServerHandler
 * 
 * @author Jimmy Hoff
 * @version 1.0
 */
@Slf4j
public class MinaServerHandler extends IoHandlerAdapter {

	@Autowired
	private BufferZone bufferZone;
	
	@Qualifier("producer")
	@Autowired
	private Counter counter;

	@Value("${spring.application.cluster.transport.bufferzone.collectionName}")
	private String collectionName;

	@Autowired(required = false)
	private ChannelEventListener<IoSession> channelEventListener;

	@Override
	public void sessionOpened(IoSession session) throws Exception {
		fireChannelEvent(session, EventType.CONNECTED, null);
	}

	@Override
	public void sessionClosed(IoSession session) throws Exception {
		fireChannelEvent(session, EventType.CLOSED, null);
	}

	@Override
	public void exceptionCaught(IoSession session, Throwable cause) throws Exception {
		log.error(cause.getMessage(), cause);
		fireChannelEvent(session, EventType.FAULTY, cause);
	}

	@Override
	public void messageReceived(IoSession session, Object message) throws Exception {
		counter.incrementCount();
		bufferZone.set(collectionName, (Tuple) message);
	}

	private void fireChannelEvent(IoSession channel, EventType eventType, Throwable cause) {
		if (channelEventListener != null) {
			channelEventListener.fireChannelEvent(new ChannelEvent<IoSession>(channel, eventType, cause));
		}
	}

}
