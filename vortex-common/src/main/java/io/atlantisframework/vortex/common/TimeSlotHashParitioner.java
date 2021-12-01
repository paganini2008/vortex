package io.atlantisframework.vortex.common;

import java.time.Instant;
import java.util.Arrays;

import com.github.paganini2008.devtools.date.TimeSlot;

/**
 * 
 * TimeSlotHashParitioner
 *
 * @author Fred Feng
 * @since 2.0.4
 */
public class TimeSlotHashParitioner extends HashPartitioner {

	private final int span;
	private final TimeSlot timeSlot;

	public TimeSlotHashParitioner(int span, TimeSlot timeSlot) {
		this.span = span;
		this.timeSlot = timeSlot;
	}

	@Override
	protected int indexFor(Tuple tuple, Object[] data, int length) {
		final int prime = 31;
		int hash = 1;
		hash = hash * prime + Arrays.deepHashCode(data);
		long timestamp = tuple.getTimestamp();
		hash = hash * prime + timeSlot.locate(Instant.ofEpochMilli(timestamp), span).hashCode();
		return (hash & 0x7FFFFFFF) % length;
	}

}
