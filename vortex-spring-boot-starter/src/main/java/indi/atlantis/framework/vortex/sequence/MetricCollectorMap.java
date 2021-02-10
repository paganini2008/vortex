package indi.atlantis.framework.vortex.sequence;

import com.github.paganini2008.devtools.collection.AtomicReferenceMap;

/**
 * 
 * MetricCollectorMap
 *
 * @author Jimmy Hoff
 * @version 1.0
 */
public class MetricCollectorMap<T extends Metric<T>> extends AtomicReferenceMap<String, T> {

	private static final long serialVersionUID = 6743810257952172449L;

	public MetricCollectorMap(boolean ordered) {
		super(ordered);
	}

	@Override
	protected T merge(String key, T current, T update) {
		if (current != null) {
			return update.reset() ? current.reset(update) : current.merge(update);
		}
		return update;
	}

}
