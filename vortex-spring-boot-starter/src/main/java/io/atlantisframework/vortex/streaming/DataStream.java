package io.atlantisframework.vortex.streaming;

import com.github.paganini2008.devtools.time.TimeSlot;
import com.github.paganini2008.devtools.time.TimeWindowListener;
import com.github.paganini2008.devtools.time.TimeWindowMap;

import io.atlantisframework.vortex.Handler;
import io.atlantisframework.vortex.common.Partitioner;
import io.atlantisframework.vortex.common.TimeSlotPartitioner;
import io.atlantisframework.vortex.common.Tuple;

/**
 * 
 * DataStream
 *
 * @author Fred Feng
 * @since 2.0.4
 */
public class DataStream<T> implements Handler {

	public DataStream(String name, Class<T> outputType) {
		this.name = name;
		this.outputType = outputType;
	}

	private final String name;
	private final Class<T> outputType;
	private TimeWindowMap<T> timeWindowMap = new TimeWindowMap<>(1, TimeSlot.MINUTE, 100, new SnapshotTimeWindowListener<>());
	private TimeSlotPartitioner partitioner = new TimeSlotPartitioner(1, TimeSlot.MINUTE);

	public void timeWindow(int span, TimeSlot timeSlot, int batchSize, TimeWindowListener<T> timeWindowListener) {
		this.timeWindowMap = new TimeWindowMap<>(span, timeSlot, batchSize, timeWindowListener);
		this.partitioner = new TimeSlotPartitioner(span, timeSlot);
	}

	public void groupingBy(String... groupingFields) {
		partitioner.groupingBy(groupingFields);
	}

	@Override
	public void onData(Tuple tuple) {
		timeWindowMap.offer(tuple.getTimestamp(), tuple.toBean(outputType));
	}

	@Override
	public String getTopic() {
		return name;
	}

	@Override
	public Partitioner getPartitioner() {
		return partitioner;
	}

}
