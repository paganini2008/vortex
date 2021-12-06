package io.atlantisframework.vortex.streaming;

import java.time.Instant;
import java.util.List;

/**
 * 
 * TimeWindowListener
 *
 * @author Fred Feng
 * @since 2.0.4
 */
public interface TimeWindowListener<V> {

	void saveCheckPoint(Instant ins, List<V> values);

}
