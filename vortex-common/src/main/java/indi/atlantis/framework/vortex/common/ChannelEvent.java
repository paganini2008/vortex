package indi.atlantis.framework.vortex.common;

import java.util.EventObject;

/**
 * 
 * ChannelEvent
 *
 * @author Fred Feng
 * @version 1.0
 */
public class ChannelEvent<T> extends EventObject {

	private static final long serialVersionUID = 6921528186565405569L;

	public ChannelEvent(T source, EventType eventType) {
		this(source, eventType, null);
	}

	public ChannelEvent(T source, EventType eventType, Throwable cause) {
		super(source);
		this.eventType = eventType;
		this.cause = cause;
	}

	private final EventType eventType;
	private final Throwable cause;

	public EventType getEventType() {
		return eventType;
	}

	public Throwable getCause() {
		return cause;
	}

	@SuppressWarnings("unchecked")
	public T getSource() {
		return (T) super.getSource();
	}

	public static enum EventType {

		CONNECTED, CLOSED, PING, PONG, FAULTY

	}

}
