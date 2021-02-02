package org.springdessert.framework.transporter.common;

import java.util.EventListener;

/**
 * 
 * ChannelEventListener
 * 
 * @author Jimmy Hoff
 * @version 1.0
 */
public interface ChannelEventListener<T> extends EventListener {

	default void fireChannelEvent(ChannelEvent<T> channelEvent) {
	}

}
