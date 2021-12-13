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
package io.atlantisframework.vortex.common;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.github.paganini2008.devtools.Assert;
import com.github.paganini2008.devtools.StringUtils;

/**
 * 
 * MultipleChoicePartitioner
 *
 * @author Fred Feng
 * @since 2.0.1
 */
public class MultipleChoicePartitioner implements Partitioner {

	private final Map<String, Partitioner> selector = new ConcurrentHashMap<String, Partitioner>();

	public MultipleChoicePartitioner() {
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
		Tuple tuple = (Tuple) data;
		Partitioner partitioner = selector.get(tuple.getTopic());
		if (partitioner == null && StringUtils.isNotBlank(tuple.getPartitionerName())) {
			partitioner = selector.get(tuple.getPartitionerName());
		}
		if (partitioner == null) {
			partitioner = defaultPartitioner;
		}
		return partitioner.selectChannel(data, channels);
	}

}
