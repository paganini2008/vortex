package indi.atlantis.framework.vortex.metric;

import java.util.Map;

/**
 * 
 * UserMetricSequencer
 * 
 * @author Fred Feng
 *
 * @version 1.0
 */
public interface UserMetricSequencer<I, V> extends MetricSequencer<I, UserMetric<V>> {

	default Map<String, Map<String, Object>> sequence(I identifier, String metric, boolean asc) {
		return sequence(identifier, new String[] { metric }, asc);
	}

	Map<String, Map<String, Object>> sequenceLatest(I identifier, String[] metrics);

	Map<String, Map<String, Object>> sequence(I identifier, String[] metrics, boolean asc);

}