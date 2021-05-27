package indi.atlantis.framework.vortex.metric;

import java.util.HashMap;
import java.util.Map;

/**
 * 
 * BoolMetricSequencer
 *
 * @author Fred Feng
 * @version 1.0
 */
public class BoolMetricSequencer extends GenericUserMetricSequencer<String, Bool> {

	public BoolMetricSequencer(MetricEvictionHandler<String, UserMetric<Bool>> evictionHandler) {
		this(1, SpanUnit.MINUTE, 60, evictionHandler);
	}

	public BoolMetricSequencer(int span, SpanUnit spanUnit, int bufferSize,
			MetricEvictionHandler<String, UserMetric<Bool>> evictionHandler) {
		super(span, spanUnit, bufferSize, evictionHandler);
	}

	@Override
	protected Map<String, Object> renderNull(long timeInMs) {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("yes", 0L);
		map.put("no", 0L);
		map.put("timestamp", timeInMs);
		return map;
	}

}
