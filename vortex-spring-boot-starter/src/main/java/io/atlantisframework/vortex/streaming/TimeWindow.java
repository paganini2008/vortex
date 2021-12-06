package io.atlantisframework.vortex.streaming;

import java.time.Instant;
import java.util.List;

/**
 * 
 * TimeWindow
 *
 * @author Fred Feng
 * @since 2.0.4
 */
public interface TimeWindow<V> {
	
	void setBatchSize(int batchSize);

	List<V> offer(long timeInMs, V payload);
	
	List<V> offer(Instant time, V payload);
	
	void clear();
	
	int size();
}
