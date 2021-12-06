package io.atlantisframework.vortex.streaming;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.github.paganini2008.devtools.collection.ConcurrentSortedBoundedMap;
import com.github.paganini2008.devtools.collection.MapUtils;
import com.github.paganini2008.devtools.multithreads.ThreadUtils;
import com.github.paganini2008.devtools.time.AccumulationTimeSlotTable;
import com.github.paganini2008.devtools.time.TimeSlot;

/**
 * 
 * TimeWindowCollector
 *
 * @author Fred Feng
 * @since 2.0.4
 */
public class TimeWindowCollector<V> implements TimeWindow<V> {

	private final TimeWindowMap<V> timeWindowMap;
	private final AccumulationTimeSlotTable<V> timeSlotTable;

	public TimeWindowCollector(int span, TimeSlot timeSlot, TimeWindowListener<V> timeWindowListener) {
		this.timeWindowMap = new TimeWindowMap<V>(timeWindowListener);
		this.timeSlotTable = new AccumulationTimeSlotTable<V>(timeWindowMap, span, timeSlot);
	}

	private int batchSize = 100;

	public void setBatchSize(int batchSize) {
		this.batchSize = batchSize;
	}

	public List<V> offer(long timeInMs, V payload) {
		return offer(Instant.ofEpochMilli(timeInMs), payload);
	}

	public List<V> offer(Instant time, V payload) {
		final List<V> values = MapUtils.get(timeSlotTable, time, () -> new CopyOnWriteArrayList<V>());
		values.add(payload);
		ThreadUtils.test(values, () -> values.size() > batchSize, () -> {
			List<V> copy = new ArrayList<>(values);
			timeWindowMap.onEviction(timeSlotTable.mutate(time), copy);
			values.removeAll(copy);
		});
		return values;
	}

	public void clear() {
		timeSlotTable.clear();
	}

	public int size() {
		return timeSlotTable.size();
	}
	
	static class TimeWindowMap<V> extends ConcurrentSortedBoundedMap<Instant, List<V>> {

		private static final long serialVersionUID = 1L;

		TimeWindowMap(TimeWindowListener<V> timeWindowListener) {
			super(1);
			this.timeWindowListener = timeWindowListener;
		}

		private final TimeWindowListener<V> timeWindowListener;

		@Override
		public void onEviction(Instant ins, List<V> values) {
			timeWindowListener.saveCheckPoint(ins, values);
		}

	}

}
