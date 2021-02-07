package indi.atlantis.framework.gearless.common;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.github.paganini2008.devtools.Assert;
import com.github.paganini2008.devtools.StringUtils;

/**
 * 
 * NamedSelectionPartitioner
 *
 * @author Jimmy Hoff
 * @version 1.0
 */
public class NamedSelectionPartitioner implements Partitioner {

	private final Map<String, Partitioner> selector = new ConcurrentHashMap<String, Partitioner>();

	public NamedSelectionPartitioner() {
		selector.put("roundrobin", new RoundRobinPartitioner());
		selector.put("random", new RandomPartitioner());
	}

	private Partitioner defaultPartitioner = new RoundRobinPartitioner();

	public void setDefaultPartitioner(Partitioner defaultPartitioner) {
		Assert.isNull(defaultPartitioner, "Default partitioner must not be null.");
		this.defaultPartitioner = defaultPartitioner;
	}

	public void addPartitioner(Partitioner partitioner) {
		addPartitioner(partitioner.getClass().getName(), partitioner);
	}

	public void addPartitioner(String name, Partitioner partitioner) {
		Assert.hasNoText(name, "Partitioner name must not be null.");
		Assert.isNull(partitioner, "Partitioner must not be null.");
		selector.put(name, partitioner);
	}

	@Override
	public <T> T selectChannel(Object data, List<T> channels) {
		if (data instanceof Tuple) {
			Tuple tuple = (Tuple) data;
			String partitionerName = tuple.getPartitionerName();
			Partitioner partitioner;
			if (StringUtils.isNotBlank(partitionerName) && null != (partitioner = selector.get(partitionerName))) {
				return partitioner.selectChannel(data, channels);
			}
		}
		return defaultPartitioner.selectChannel(data, channels);
	}

}
