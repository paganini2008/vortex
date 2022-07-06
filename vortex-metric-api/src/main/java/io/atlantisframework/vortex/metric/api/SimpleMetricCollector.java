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

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 
 * SimpleMetricCollector
 *
 * @author Fred Feng
 * @since 2.0.4
 */
public class SimpleMetricCollector<M, T extends Metric<T>> implements MetricCollector<M, T> {

	public SimpleMetricCollector() {
		this(-1, null);
	}

	public SimpleMetricCollector(int bufferSize, HistoricalMetricsHandler<M, T> historicalMetricsHandler) {
		this.store = bufferSize > 0 ? new SortedBoundedMetricMap<>(bufferSize, historicalMetricsHandler) : new SortedMetricMap<>();
	}

	private final MetricMap<M, T> store;
	
	@Override
	public T set(M metric, T metricUnit, boolean merged) {
		return merged ? store.merge(metric, metricUnit) : store.put(metric, metricUnit);
	}

	@Override
	public T get(M metric) {
		return store.get(metric);
	}

	@Override
	public Collection<M> metrics() {
		return Collections.unmodifiableCollection(store.keySet());
	}

	@Override
	public Map<M, T> all() {
		return Collections.unmodifiableMap(new LinkedHashMap<M, T>(store));
	}

	@Override
	public int size() {
		return store.size();
	}

	@Override
	public void clear() {
		store.clear();
	}

}
