package indi.atlantis.framework.vortex.metric;

import lombok.extern.slf4j.Slf4j;

/**
 * 
 * LoggingMetricEvictionHandler
 * 
 * @author Jimmy Hoff
 *
 * @version 1.0
 */
@Slf4j
public class LoggingMetricEvictionHandler<V> implements MetricEvictionHandler<String, UserMetric<V>> {

	@Override
	public void onEldestMetricRemoval(String identifier, String eldestMetric, UserMetric<V> eldestMetricUnit) {
		if (log.isTraceEnabled()) {
			log.trace("Discard metric data: {}/{}/{}", identifier, eldestMetric, eldestMetricUnit);
		}
	}

}
