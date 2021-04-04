package indi.atlantis.framework.vortex.metric;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.github.paganini2008.devtools.collection.MapUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * 
 * UserSequencer
 * 
 * @author Jimmy Hoff
 *
 * @version 1.0
 */
@Slf4j
public final class UserSequencer {

	private final Map<String, UserMetricSequencer<String, ?>> registerMap = new ConcurrentHashMap<String, UserMetricSequencer<String, ?>>();

	public Map<String, Map<String, Object>> sequence(String dataType, String identifier, String metric, boolean asc) {
		return sequence(dataType, identifier, new String[] { metric }, asc);
	}

	public Map<String, Map<String, Object>> sequence(String dataType, String identifier, String[] metrics, boolean asc) {
		return registerMap.containsKey(dataType) ? registerMap.get(dataType).sequence(identifier, metrics, asc) : MapUtils.emptyMap();
	}

	public void registerDataType(String dataType, UserMetricSequencer<String, ?> metricSequencer) {
		registerMap.put(dataType, metricSequencer);
		log.info("Add metric sequencer '{}' with type '{}'.", metricSequencer.getClass().getName(), dataType);
	}

	public void removeDataType(String dataType) {
		if (registerMap.remove(dataType) != null) {
			log.info("Remove metric sequencer with type '{}'.", dataType);
		}

	}
}
