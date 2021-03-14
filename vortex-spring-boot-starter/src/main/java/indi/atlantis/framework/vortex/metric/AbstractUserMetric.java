package indi.atlantis.framework.vortex.metric;

import lombok.ToString;

/**
 * 
 * AbstractUserMetric
 * 
 * @author Jimmy Hoff
 *
 * @version 1.0
 */
@ToString
public abstract class AbstractUserMetric<V> implements UserMetric<V> {

	private final V value;
	private final long timestamp;
	private final boolean reset;

	protected AbstractUserMetric(V value, long timestamp, boolean reset) {
		this.value = value;
		this.timestamp = timestamp;
		this.reset = reset;
	}

	@Override
	public long getTimestamp() {
		return timestamp;
	}

	@Override
	public boolean reset() {
		return reset;
	}

	@Override
	public V get() {
		return value;
	}

	public ResettableUserMetric<V> resettable() {
		return new ResettableUserMetric<V>(this);
	}

}
