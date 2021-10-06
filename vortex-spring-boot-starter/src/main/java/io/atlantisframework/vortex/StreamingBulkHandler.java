/**
* Copyright 2017-2021 Fred Feng (paganini.fy@gmail.com)

* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package io.atlantisframework.vortex;

import java.util.List;
import java.util.stream.Collectors;

import com.github.paganini2008.devtools.beans.streaming.Selector;
import com.github.paganini2008.devtools.collection.CollectionUtils;

import io.atlantisframework.vortex.common.Tuple;

/**
 * 
 * StreamingBulkHandler
 *
 * @author Fred Feng
 * @since 2.0.1
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
