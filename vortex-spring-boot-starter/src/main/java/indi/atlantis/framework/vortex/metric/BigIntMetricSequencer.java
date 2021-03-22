package indi.atlantis.framework.vortex.metric;

import java.util.HashMap;
import java.util.Map;

/**
 * 
 * BigIntMetricSequencer
 * 
 * @author Jimmy Hoff
 *
 * @version 1.0
 */
public class BigIntMetricSequencer extends GenericUserMetricSequencer<String, BigInt> {

	public BigIntMetricSequencer(MetricEvictionHandler<String, UserMetric<BigInt>> evictionHandler) {
		this(1, SpanUnit.MINUTE, 60, evictionHandler);
	}

	public BigIntMetricSequencer(int span, SpanUnit spanUnit, int bufferSize,
			MetricEvictionHandler<String, UserMetric<BigInt>> evictionHandler) {
		super(span, spanUnit, bufferSize, evictionHandler);
	}

	@Override
	protected Map<String, Object> renderNull(long timeInMs) {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("highestValue", 0L);
		map.put("middleValue", 0L);
		map.put("lowestValue", 0L);
		map.put("count", 0);
		map.put("timestamp", timeInMs);
		return map;
	}

}