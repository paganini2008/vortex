package indi.atlantis.framework.vortex.metric;

import java.util.Map;

/**
 * 
 * ResettableUserMetric
 * 
 * @author Jimmy Hoff
 *
 * @version 1.0
 */
public class ResettableUserMetric<V> implements UserMetric<V> {

	private final UserMetric<V> real;

	ResettableUserMetric(UserMetric<V> real) {
		this.real = real;
	}

	@Override
	public long getTimestamp() {
		return real.getTimestamp();
	}

	@Override
	public boolean reset() {
		return Boolean.TRUE;
	}

	@Override
	public UserMetric<V> reset(UserMetric<V> currentMetric) {
		return real.reset(currentMetric);
	}

	@Override
	public UserMetric<V> merge(UserMetric<V> anotherMetric) {
		return real.merge(anotherMetric);
	}

	@Override
	public V get() {
		return real.get();
	}

	@Override
	public Map<String, Object> toEntries() {
		return real.toEntries();
	}

}
