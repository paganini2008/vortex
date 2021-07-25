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
package indi.atlantis.framework.vortex.metric;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import indi.atlantis.framework.vortex.utils.HistoricalMetricsHandler;

/**
 * 
 * SimpleMetricCollector
 *
 * @author Fred Feng
 * @version 1.0
 */
public class SimpleMetricCollector<T extends Metric<T>> implements MetricCollector<T> {

	private final Map<String, T> store;

	public SimpleMetricCollector() {
		this(-1, null);
	}

	public SimpleMetricCollector(int bufferSize, HistoricalMetricsHandler<T> historicalMetricsHandler) {
		this(bufferSize, true, historicalMetricsHandler);
	}

	public SimpleMetricCollector(int bufferSize, boolean ordered, HistoricalMetricsHandler<T> historicalMetricsHandler) {
		this.store = bufferSize > 0 ? new MetricCollectorBoundedMap<T>(ordered, bufferSize, historicalMetricsHandler)
				: new MetricCollectorMap<T>(ordered);
	}

	@Override
	public T set(String metric, T metricUnit, boolean merged) {
		if (merged) {
			return store.putIfAbsent(metric, metricUnit);
		}
		return store.put(metric, metricUnit);
	}

	@Override
	public T get(String metric) {
		return store.get(metric);
	}

	@Override
	public String[] metrics() {
		return store.keySet().toArray(new String[0]);
	}

	@Override
	public Map<String, T> all() {
		return Collections.unmodifiableMap(new LinkedHashMap<String, T>(store));
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
