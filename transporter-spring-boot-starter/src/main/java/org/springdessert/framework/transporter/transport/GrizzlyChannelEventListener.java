package org.springdessert.framework.transporter.transport;

import org.glassfish.grizzly.Connection;

import org.springdessert.framework.transporter.common.ChannelEvent;
import org.springdessert.framework.transporter.common.ChannelEventListener;
import org.springdessert.framework.transporter.common.ChannelEvent.EventType;

import lombok.extern.slf4j.Slf4j;

/**
 * 
 * GrizzlyChannelEventListener
 *
 * @author Jimmy Hoff
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
