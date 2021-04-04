package indi.atlantis.framework.vortex.metric;

import java.util.HashMap;
import java.util.Map;

/**
 * 
 * BigIntMetric
 * 
 * @author Jimmy Hoff
 *
 * @version 1.0
 */
public class BigIntMetric extends AbstractUserMetric<BigInt> {

	public BigIntMetric(long value, long timestamp) {
		this(new BigInt(value), timestamp);
	}

	public BigIntMetric(BigInt bigInt, long timestamp) {
		super(bigInt, timestamp, false);
	}

	@Override
	public UserMetric<BigInt> reset(UserMetric<BigInt> newMetric) {
		BigInt current = get();
		BigInt update = newMetric.get();
		long highestValue = Long.max(current.getHighestValue(), update.getHighestValue());
		long lowestValue = Long.min(current.getLowestValue(), update.getLowestValue());
		long totalValue = current.getTotalValue() - update.getTotalValue();
		long count = current.getCount() - update.getCount();
		long timestamp = newMetric.getTimestamp();
		return new BigIntMetric(new BigInt(highestValue, lowestValue, totalValue, count), timestamp);
	}

	@Override
	public UserMetric<BigInt> merge(UserMetric<BigInt> newMetric) {
		BigInt current = get();
		BigInt update = newMetric.get();
		long highestValue = Long.max(current.getHighestValue(), update.getHighestValue());
		long lowestValue = Long.min(current.getLowestValue(), update.getLowestValue());
		long totalValue = current.getTotalValue() + update.getTotalValue();
		long count = current.getCount() + update.getCount();
		long timestamp = newMetric.getTimestamp();
		return new BigIntMetric(new BigInt(highestValue, lowestValue, totalValue, count), timestamp);
	}

	@Override
	public Map<String, Object> toEntries() {
		BigInt bigInt = get();
		Map<String, Object> data = new HashMap<String, Object>(5);
		data.put("highestValue", bigInt.getHighestValue());
		data.put("lowestValue", bigInt.getLowestValue());
		data.put("middleValue", bigInt.getMiddleValue());
		data.put("count", bigInt.getCount());
		data.put("timestamp", getTimestamp());
		return data;
	}

}
