package io.atlantisframework.vortex.streaming;

import java.time.Instant;
import java.util.List;

import com.github.paganini2008.devtools.time.TimeWindowListener;

import lombok.extern.slf4j.Slf4j;

/**
 * 
 * SnapshotTimeWindowListener
 *
 * @author Fred Feng
 * @since 2.0.4
 */
@Slf4j
public class SnapshotTimeWindowListener<T> implements TimeWindowListener<T> {

	@Override
	public void saveCheckPoint(Instant time, List<T> values) {
		if (log.isInfoEnabled()) {
			log.info("[{}] show info: {}", time.toString(), values.stream().findAny().get());
		}
	}

}
