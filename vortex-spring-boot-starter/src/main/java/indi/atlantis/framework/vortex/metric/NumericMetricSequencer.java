package indi.atlantis.framework.vortex.metric;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * 
 * NumericMetricSequencer
 * 
 * @author Jimmy Hoff
 *
 * @version 1.0
 */
public class NumericMetricSequencer extends GenericUserMetricSequencer<String, Numeric> {

	public NumericMetricSequencer(MetricEvictionHandler<String, UserMetric<Numeric>> evictionHandler) {
		this(1, SpanUnit.MINUTE, 60, evictionHandler);
	}

	public NumericMetricSequencer(int span, SpanUnit spanUnit, int bufferSize,
			MetricEvictionHandler<String, UserMetric<Numeric>> evictionHandler) {
		super(span, spanUnit, bufferSize, evictionHandler);
	}

	@Override
	protected Map<String, Object> renderNull(long timeInMs) {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("highestValue", BigDecimal.ZERO);
		map.put("middleValue", BigDecimal.ZERO);
		map.put("lowestValue", BigDecimal.ZERO);
		map.put("count", 0L);
		map.put("timestamp", timeInMs);
		return map;
	}

}
