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

import com.github.paganini2008.devtools.collection.ConcurrentSortedBoundedMap;

/**
 * 
 * SortedBoundedMetricMap
 *
 * @author Fred Feng
 * @since 2.0.4
 */
public class SortedBoundedMetricMap<M, T extends Metric<T>> extends ConcurrentSortedBoundedMap<M, T> implements MetricMap<M, T> {

	private static final long serialVersionUID = 7381951980154645393L;

	public SortedBoundedMetricMap(int maxSize, HistoricalMetricsHandler<M, T> historicalMetricsHandler) {
		super(new SortedMetricMap<>(), maxSize);
		this.historicalMetricsHandler = historicalMetricsHandler;
	}

	private HistoricalMetricsHandler<M, T> historicalMetricsHandler;

	@Override
	public T merge(M key, T value) {
		return super.merge(key, value, (current, update) -> {
			if (current != null) {
				return update.reset() ? current.reset(update) : current.merge(update);
			}
			return update;
		});
	}

	@Override
	public void onEviction(M eldestKey, T eldestValue) {
		if (historicalMetricsHandler != null) {
			historicalMetricsHandler.handleHistoricalMetrics(eldestKey, eldestValue);
		}
	}

}
