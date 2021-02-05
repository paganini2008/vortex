package org.springtribe.framework.gearless.utils;

import com.github.paganini2008.devtools.collection.AtomicReferenceMap;

/**
 * 
 * MetricsCollectorMap
 *
 * @author Jimmy Hoff
 * @version 1.0
 */
public class MetricsCollectorMap<T extends Metric<T>> extends AtomicReferenceMap<String, T> {

	private static final long serialVersionUID = 6743810257952172449L;

	public MetricsCollectorMap(boolean ordered) {
		super(ordered);
	}

	@Override
	protected T merge(String key, T current, T update) {
		if (current != null) {
			return update.reset() ? current.reset(update) : current.merge(update);
		} else {
			return update;
		}
	}

}
