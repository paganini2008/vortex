package indi.atlantis.framework.vortex.transport;

import org.glassfish.grizzly.Connection;

import indi.atlantis.framework.vortex.common.ChannelEvent;
import indi.atlantis.framework.vortex.common.ChannelEventListener;
import indi.atlantis.framework.vortex.common.ChannelEvent.EventType;
import lombok.extern.slf4j.Slf4j;

/**
 * 
 * GrizzlyChannelEventListener
 *
 * @author Fred Feng
 * @version 1.0
 */
@Slf4j
public class GrizzlyChannelEventListener implements ChannelEventListener<Connection<?>>{

	@Override
	public void fireChannelEvent(ChannelEvent<Connection<?>> channelEvent) {
		if (log.isTraceEnabled()) {
			Connection<?> connection = channelEvent.getSource();
			EventType eventType = channelEvent.getEventType();
			switch (eventType) {
			case CONNECTED:
				log.trace(connection.getPeerAddress() + " has established connection.");
				break;
			case CLOSED:
				log.trace(connection.getPeerAddress() + " has loss connection.");
				break;
			case PING:
				log.trace(connection.getPeerAddress() + " send a ping.");
				break;
			case PONG:
				log.trace(connection.getPeerAddress() + " send a pong.");
				break;
			case FAULTY:
				log.trace(connection.getPeerAddress() + " has loss connection for fatal reason.", channelEvent.getCause());
				break;
			}
		}
	}

}
