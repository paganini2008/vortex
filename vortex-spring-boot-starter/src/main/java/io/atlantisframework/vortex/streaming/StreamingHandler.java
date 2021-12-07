package io.atlantisframework.vortex.streaming;

import com.github.paganini2008.devtools.time.TimeSlot;

import io.atlantisframework.vortex.Handler;
import io.atlantisframework.vortex.common.Tuple;

/**
 * 
 * StreamingHandler
 *
 * @author Fred Feng
 * @since 2.0.4
 */

public class StreamingHandler<T> implements Handler {

	private final String name;
	private final Class<T> requiredClass;

	public StreamingHandler(String name, Class<T> requiredClass) {
		this.name = name;
		this.requiredClass = requiredClass;
	}

	private TimeWindow<T> timeWindow;

	public StreamingHandler<T> timeWindow(int span, TimeSlot timeSlot, TimeWindowListener<T> timeWindowListener) {
		this.timeWindow = new TimeWindowCollector<T>(span, timeSlot, timeWindowListener);
		return this;
	}

	@Override
	public void onData(Tuple tuple) {
		timeWindow.offer(tuple.getTimestamp(), tuple.toBean(requiredClass));
	}

	@Override
	public String getTopic() {
		return name;
	}

}
