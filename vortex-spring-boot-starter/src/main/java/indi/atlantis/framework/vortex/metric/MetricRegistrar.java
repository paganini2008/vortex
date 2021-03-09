package indi.atlantis.framework.vortex.metric;

import java.util.Map;
import java.util.function.Function;

import indi.atlantis.framework.vortex.Handler;

/**
 * 
 * MetricRegistrar
 * 
 * @author Jimmy Hoff
 *
 * @version 1.0
 */
public interface MetricRegistrar {

	void configure(int span, SpanUnit spanUnit, int bufferSize);

	Sequencer getSequencer();

	Handler getHandler();

	Handler getSynchronizationHandler();

	Handler getIncrementalSynchronizationHandler();

	Synchronizer getSynchronizer();

	Synchronizer getIncrementalSynchronizer();

	default Function<Long, Map<String, Object>> getRender() {
		return null;
	}

}
