package indi.atlantis.framework.vortex.sequence;

import java.util.Map;
import java.util.function.Supplier;

import indi.atlantis.framework.vortex.sequence.SpanUnit;

/**
 * 
 * ChartDataRender
 *
 * @author Jimmy Hoff
 * @version 1.0
 */
public abstract class ChartDataRender {

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
