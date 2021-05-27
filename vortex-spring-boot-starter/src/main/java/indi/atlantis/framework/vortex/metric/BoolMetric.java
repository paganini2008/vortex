package indi.atlantis.framework.vortex.metric;

import java.util.HashMap;
import java.util.Map;

/**
 * 
 * BoolMetric
 *
 * @author Fred Feng
 * @version 1.0
 */
public class BoolMetric extends AbstractUserMetric<Bool> {

	public BoolMetric(boolean yes, long timestamp) {
		this(new Bool(yes), timestamp);
	}

	public BoolMetric(boolean yes, boolean no, long timestamp) {
		this(new Bool(yes, no), timestamp);
	}

	public BoolMetric(Bool bool, long timestamp) {
		super(bool, timestamp, false);
	}

	@Override
	public UserMetric<Bool> reset(UserMetric<Bool> newMetric) {
		Bool current = get();
		Bool update = newMetric.get();
		long yes = current.getYes() - update.getYes();
		long no = current.getNo() - update.getNo();
		Bool bool = new Bool(yes, no);
		return new BoolMetric(bool, newMetric.getTimestamp());
	}

	@Override
	public UserMetric<Bool> merge(UserMetric<Bool> newMetric) {
		Bool current = get();
		Bool update = newMetric.get();
		long yes = current.getYes() + update.getYes();
		long no = current.getNo() + update.getNo();
		Bool bool = new Bool(yes, no);
		return new BoolMetric(bool, newMetric.getTimestamp());
	}

	public Map<String, Object> toEntries() {
		Bool bool = get();
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("yes", bool.getYes());
		map.put("no", bool.getNo());
		map.put("timestamp", getTimestamp());
		return map;
	}

}
