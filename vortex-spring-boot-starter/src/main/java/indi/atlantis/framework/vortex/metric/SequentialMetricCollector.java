/**
* Copyright 2021 Fred Feng (paganini.fy@gmail.com)

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

import java.util.LinkedHashMap;
import java.util.Map;

import com.github.paganini2008.devtools.collection.MapUtils;

/**
 * 
 * SequentialMetricCollector
 *
 * @author Fred Feng
 * @version 1.0
 */
public interface SequentialMetricCollector<T extends Metric<T>> extends MetricCollector<T> {

	static final String DEFAULT_DATETIME_PATTERN = "HH:mm:ss";

	default T set(String metric, T metricUnit, boolean merged) {
		return set(metric, Long.min(System.currentTimeMillis(), metricUnit.getTimestamp()), metricUnit, merged);
	}

	T set(String metric, long timestamp, T metricUnit, boolean merged);

	default T get(String metric) {
		Map<String, T> data = sequence(metric);
		Map.Entry<String, T> lastEntry = MapUtils.getLastEntry(data);
		return lastEntry != null ? lastEntry.getValue() : null;
	}

	default Map<String, T> all() {
		Map<String, T> data = new LinkedHashMap<String, T>();
		for (String metric : metrics()) {
			data.put(metric, get(metric));
		}
		return data;
	}

	Map<String, T> sequence(String metric);

}
