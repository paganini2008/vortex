package indi.atlantis.framework.vortex.metric;

import java.util.LinkedHashMap;
import java.util.Map;

import com.github.paganini2008.devtools.collection.MapUtils;

/**
 * 
 * SequentialMetricCollector
 *
 * @author Jimmy Hoff
 * @version 1.0
 */
public interface SequentialMetricCollector<T extends Metric<T>> extends MetricCollector<T> {

	static final String DEFAULT_DATETIME_PATTERN = "HH:mm:ss";

	default T set(String metric, T metricUnit, boolean merged) {
		return set(metric, Long.min(System.currentTimeMillis(), metricUnit.getTimestamp()), metricUnit, merged);
	}

	T set(String metric, long timestamp, T metricUnit, boolean merged);

	default T get(String metric) {
		Map<String, T> data = sequence(metric);
		Map.Entry<String, T> lastEntry = MapUtils.getLastEntry(data);
		return lastEntry != null ? lastEntry.getValue() : null;
	}

	default Map<String, T> all() {
		Map<String, T> data = new LinkedHashMap<String, T>();
		for (String metric : metrics()) {
			data.put(metric, get(metric));
		}
		return data;
	}

	Map<String, T> sequence(String metric);

}
