package indi.atlantis.framework.vortex.transport;

import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import indi.atlantis.framework.vortex.Accumulator;
import indi.atlantis.framework.vortex.buffer.BufferZone;
import indi.atlantis.framework.vortex.common.ChannelEvent;
import indi.atlantis.framework.vortex.common.ChannelEvent.EventType;
import indi.atlantis.framework.vortex.common.ChannelEventListener;
import indi.atlantis.framework.vortex.common.Tuple;
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

	@Autowired
	private Accumulator accumulator;

	@Value("${atlantis.framework.vortex.bufferzone.collectionName}")
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
		bufferZone.set(collectionName, (Tuple) message);
		accumulator.accumulate((Tuple) message);
	}

	private void fireChannelEvent(IoSession channel, EventType eventType, Throwable cause) {
		if (channelEventListener != null) {
			channelEventListener.fireChannelEvent(new ChannelEvent<IoSession>(channel, eventType, cause));
		}
	}

}
