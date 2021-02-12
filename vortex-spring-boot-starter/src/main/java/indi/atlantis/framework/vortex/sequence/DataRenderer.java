package indi.atlantis.framework.vortex.sequence;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * 
 * DataRenderer
 *
 * @author Jimmy Hoff
 * @version 1.0
 */
public abstract class DataRenderer {

	public static <T> Map<String, Map<String, Object>> renderBoolMetric(Map<String, Map<String, Object>> data, SpanUnit spanUnit, int span,
			int bufferSize) {
		return render(data, spanUnit, span, bufferSize, () -> {
			Map<String, Object> map = new HashMap<String, Object>();
			map.put("yes", 0L);
			map.put("no", 0L);
			return map;
		});
	}

	public static <T> Map<String, Map<String, Object>> renderNumberMetric(Map<String, Map<String, Object>> data, SpanUnit spanUnit,
			int span, int bufferSize) {
		return render(data, spanUnit, span, bufferSize, () -> {
			Map<String, Object> map = new HashMap<String, Object>();
			map.put("highestValue", 0L);
			map.put("middleValue", 0L);
			map.put("lowestValue", 0L);
			map.put("count", 0);
			return map;
		});
	}

	public static <T> Map<String, T> render(Map<String, T> data, SpanUnit spanUnit, int span, int bufferSize, Supplier<T> valueSupplier) {
		Map<String, T> sequentialMap = spanUnit.newSequentialMap(span, bufferSize, valueSupplier);
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
