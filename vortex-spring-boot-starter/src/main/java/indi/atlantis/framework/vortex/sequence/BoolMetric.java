package indi.atlantis.framework.vortex.sequence;

import lombok.ToString;

/**
 * 
 * BoolMetric
 *
 * @author Jimmy Hoff
 * @version 1.0
 */
@ToString
public class BoolMetric implements UserMetric<Bool> {

	private Bool bool;
	private long timestamp;
	private final boolean reset;

	public BoolMetric(boolean is, long timestamp) {
		this(is, !is, timestamp);
	}

	public BoolMetric(boolean yes, boolean no, long timestamp) {
		this(new Bool(yes ? 1L : 0L, no ? 1L : 0L), timestamp, false);
	}

	public BoolMetric(Bool bool, long timestamp, boolean reset) {
		this.bool = bool;
		this.timestamp = timestamp;
		this.reset = reset;
	}

	@Override
	public boolean reset() {
		return this.reset;
	}

	@Override
	public long getTimestamp() {
		return timestamp;
	}

	@Override
	public UserMetric<Bool> reset(UserMetric<Bool> currentMetric) {
		Bool current = this.get();
		Bool update = currentMetric.get();
		long yes = current.getYes() - update.getYes();
		long no = current.getNo() - update.getNo();
		Bool bool = new Bool(yes, no);
		return new BoolMetric(bool, currentMetric.getTimestamp(), false);
	}

	@Override
	public UserMetric<Bool> merge(UserMetric<Bool> anotherMetric) {
		Bool current = this.get();
		Bool update = anotherMetric.get();
		long yes = current.getYes() + update.getYes();
		long no = current.getNo() + update.getNo();
		Bool bool = new Bool(yes, no);
		return new BoolMetric(bool, anotherMetric.getTimestamp(), false);
	}

	@Override
	public Bool get() {
		return bool;
	}

}
