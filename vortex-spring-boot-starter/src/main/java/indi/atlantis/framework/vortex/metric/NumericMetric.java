package indi.atlantis.framework.vortex.metric;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * 
 * NumericMetric
 * 
 * @author Fred Feng
 *
 * @version 1.0
 */
public class NumericMetric extends AbstractUserMetric<Numeric> {

	public NumericMetric(BigDecimal value, long timestamp) {
		this(new Numeric(value), timestamp);
	}

	public NumericMetric(Numeric numeric, long timestamp) {
		super(numeric, timestamp, false);
	}

	@Override
	public UserMetric<Numeric> reset(UserMetric<Numeric> newMetric) {
		Numeric current = get();
		Numeric update = newMetric.get();
		BigDecimal highestValue = current.getHighestValue().max(update.getHighestValue());
		BigDecimal lowestValue = current.getLowestValue().min(update.getLowestValue());
		BigDecimal totalValue = current.getTotalValue().subtract(update.getTotalValue());
		long count = current.getCount() - update.getCount();
		long timestamp = newMetric.getTimestamp();
		return new NumericMetric(new Numeric(highestValue, lowestValue, totalValue, count), timestamp);
	}

	@Override
	public UserMetric<Numeric> merge(UserMetric<Numeric> newMetric) {
		Numeric current = get();
		Numeric update = newMetric.get();
		BigDecimal highestValue = current.getHighestValue().max(update.getHighestValue());
		BigDecimal lowestValue = current.getLowestValue().min(update.getLowestValue());
		BigDecimal totalValue = current.getTotalValue().add(update.getTotalValue());
		long count = current.getCount() + update.getCount();
		long timestamp = newMetric.getTimestamp();
		return new NumericMetric(new Numeric(highestValue, lowestValue, totalValue, count), timestamp);
	}

	@Override
	public Map<String, Object> toEntries() {
		Numeric numeric = get();
		Map<String, Object> data = new HashMap<String, Object>(5);
		data.put("highestValue", numeric.getHighestValue());
		data.put("lowestValue", numeric.getLowestValue());
		data.put("middleValue", numeric.getMiddleValue());
		data.put("count", numeric.getCount());
		data.put("timestamp", getTimestamp());
		return data;
	}

}
