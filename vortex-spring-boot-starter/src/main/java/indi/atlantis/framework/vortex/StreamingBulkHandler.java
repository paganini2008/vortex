package indi.atlantis.framework.vortex;

import java.util.List;
import java.util.stream.Collectors;

import com.github.paganini2008.devtools.beans.streaming.Selector;
import com.github.paganini2008.devtools.collection.CollectionUtils;

import indi.atlantis.framework.vortex.common.Tuple;

/**
 * 
 * StreamingBulkHandler
 *
 * @author Fred Feng
 * @version 1.0
 */
public abstract class StreamingBulkHandler<T> implements BulkHandler {

	private final Class<T> requiredType;

	public StreamingBulkHandler(Class<T> requiredType) {
		this.requiredType = requiredType;
	}

	@Override
	public final void onBatch(String topic, List<Tuple> list) {
		if (CollectionUtils.isEmpty(list)) {
			return;
		}
		List<T> dataList = list.stream().collect(Collectors.mapping(tuple -> tuple.toBean(requiredType), Collectors.toList()));
		forBulk(Selector.from(dataList));
	}

	protected abstract void forBulk(Selector<T> selector);

}
