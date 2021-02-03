package org.springtribe.framework.gearless;

import java.util.ArrayList;
import java.util.List;

import org.springtribe.framework.gearless.common.Tuple;

import com.github.paganini2008.devtools.beans.streaming.Selector;
import com.github.paganini2008.devtools.collection.CollectionUtils;

/**
 * 
 * StreamingBulkHandler
 *
 * @author Jimmy Hoff
 * @version 1.0
 */
public abstract class StreamingBulkHandler<T> implements BulkHandler {

	private final Class<T> requiredType;

	public StreamingBulkHandler(Class<T> requiredType) {
		this.requiredType = requiredType;
	}

	@Override
	public final void onBatch(List<Tuple> list) {
		if (CollectionUtils.isEmpty(list)) {
			return;
		}
		List<T> dataList = new ArrayList<T>();
		list.forEach(tuple -> {
			dataList.add(tuple.toBean(requiredType));
		});
		forBulk(Selector.from(dataList));
	}

	protected abstract void forBulk(Selector<T> selector);

}
