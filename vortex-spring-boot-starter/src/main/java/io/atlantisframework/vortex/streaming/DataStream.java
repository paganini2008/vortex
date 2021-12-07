package io.atlantisframework.vortex.streaming;

import java.util.function.Function;

import com.github.paganini2008.devtools.Observable;
import com.github.paganini2008.devtools.time.TimeSlot;

import io.atlantisframework.vortex.common.Tuple;

/**
 * 
 * DataStream
 *
 * @author Fred Feng
 *
 * @since 2.0.4
 */
public class DataStream<T> extends Observable {

	private final String name;
	private final Class<T> beanClass;

	public DataStream(String name, Class<T> beanClass) {
		super(true);
		this.name = name;
		this.beanClass = beanClass;
	}

	private TimeWindow<T> timeWindow;

	public DataStream<T> timeWindow(int span, TimeSlot timeSlot, TimeWindowListener<T> timeWindowListener) {
		this.timeWindow = new TimeWindowCollector<T>(span, timeSlot, timeWindowListener);
		return this;
	}

	public DataStream<T> flatMap(Function<T, Object>... functions) {
		this.addObserver((ob, arg) -> {
			Tuple tuple = (Tuple) arg;
			T bean = tuple.toBean(beanClass);
			timeWindow.offer(tuple.getTimestamp(), bean);
		});
		return this;
	}

	public static DataStream of(String name) {

	}

}
