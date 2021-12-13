package io.atlantisframework.vortex.metric.api;

import java.util.concurrent.ConcurrentSkipListMap;

import com.github.paganini2008.devtools.collection.AtomicMutableMap;

import io.atlantisframework.vortex.metric.Metric;

/**
 * 
 * SimpleMetricMap
 *
 * @author Fred Feng
 * @since 2.0.4
 */
public class SimpleMetricMap<M, T extends Metric<T>> extends AtomicMutableMap<M, T> implements MetricMap<M, T> {

	private static final long serialVersionUID = 1753463886093156823L;

	public SimpleMetricMap() {
		super(new ConcurrentSkipListMap<>());
	}

	@Override
	public T merge(M key, T value) {
		return super.merge(key, value, (current, update) -> {
			if (current != null) {
				return update.reset() ? current.reset(update) : current.merge(update);
			}
			return update;
		});
	}

	@SuppressWarnings("unchecked")
	@Override
	protected final M mutate(Object key) {
		return (M) key;
	}

}
