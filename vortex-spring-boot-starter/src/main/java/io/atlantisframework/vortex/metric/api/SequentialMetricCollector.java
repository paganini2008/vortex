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
package io.atlantisframework.vortex.metric.api;

import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import com.github.paganini2008.devtools.collection.MapUtils;

/**
 * 
 * SequentialMetricCollector
 *
 * @author Fred Feng
 * @since 2.0.4
 */
public interface SequentialMetricCollector<M, T extends Metric<T>>  {

	default T set(M metric, T metricUnit, boolean merged) {
		return set(metric, Instant.ofEpochMilli(Long.min(System.currentTimeMillis(), metricUnit.getTimestamp())), metricUnit, merged);
	}

	T set(M metric, Instant instant, T metricUnit, boolean merged);

	default T get(M metric) {
		Map<Instant, T> data = sequence(metric);
		Map.Entry<Instant, T> lastEntry = MapUtils.getLastEntry(data);
		return lastEntry != null ? lastEntry.getValue() : null;
	}

	default Map<M, T> all() {
		Map<M, T> data = new LinkedHashMap<M, T>();
		for (M metric : metrics()) {
			data.put(metric, get(metric));
		}
		return data;
	}
	
	Collection<M> metrics();

	int size();

	void clear();

	Map<Instant, T> sequence(M metric);

}
