package indi.atlantis.framework.vortex.sequence;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * 
 * DataRenderer
 *
 * @author Jimmy Hoff
 * @version 1.0
 */
public abstract class DataRenderer {

	public static <T> Map<String, Map<String, Object>> renderBoolMetric(Map<String, Map<String, Object>> data, Date startTime, boolean asc,
			SpanUnit spanUnit, int span, int bufferSize) {
		return render(data, startTime, asc, spanUnit, span, bufferSize, time -> {
			Map<String, Object> map = new HashMap<String, Object>();
			map.put("yes", 0L);
			map.put("no", 0L);
			map.put("timestamp", time);
			return map;
		});
	}

	public static <T> Map<String, Map<String, Object>> renderNumberMetric(Map<String, Map<String, Object>> data, Date startTime,
			boolean asc, SpanUnit spanUnit, int span, int bufferSize) {
		return render(data, startTime, asc, spanUnit, span, bufferSize, time -> {
			Map<String, Object> map = new HashMap<String, Object>();
			map.put("highestValue", 0L);
			map.put("middleValue", 0L);
			map.put("lowestValue", 0L);
			map.put("count", 0);
			map.put("timestamp", time);
			return map;
		});
	}

	public static <T> Map<String, T> render(Map<String, T> data, Date startTime, boolean asc, SpanUnit spanUnit, int span, int bufferSize,
			Function<Long, T> f) {
		Map<String, T> sequentialMap = asc ? spanUnit.ascendingMap(startTime, span, bufferSize, f)
				: spanUnit.descendingMap(startTime, span, bufferSize, f);
		String datetime;
		for (Map.Entry<String, T> entry : data.entrySet()) {
			datetime = entry.getKey();
			if (sequentialMap.containsKey(datetime)) {
				sequentialMap.put(datetime, entry.getValue());
			}
		}
		return sequentialMap;
	}

}
