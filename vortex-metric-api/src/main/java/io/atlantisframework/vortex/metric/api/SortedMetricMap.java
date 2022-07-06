/**
* Copyright 2017-2022 Fred Feng (paganini.fy@gmail.com)

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
package io.atlantisframework.vortex.metric.api;

import java.util.concurrent.ConcurrentSkipListMap;

import com.github.paganini2008.devtools.collection.AtomicMutableMap;

/**
 * 
 * SortedMetricMap
 *
 * @author Fred Feng
 * @since 2.0.4
 */
public class SortedMetricMap<M, T extends Metric<T>> extends AtomicMutableMap<M, T> implements MetricMap<M, T> {

	private static final long serialVersionUID = 1753463886093156823L;

	public SortedMetricMap() {
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

}
