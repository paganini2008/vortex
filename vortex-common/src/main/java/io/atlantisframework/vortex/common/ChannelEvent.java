/**
* Copyright 2017-2022 Fred Feng (paganini.fy@gmail.com)

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
package io.atlantisframework.vortex.common;

import java.util.EventObject;

/**
 * 
 * ChannelEvent
 *
 * @author Fred Feng
 * @since 2.0.1
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
