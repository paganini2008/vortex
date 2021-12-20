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
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import com.github.paganini2008.devtools.Assert;
import com.github.paganini2008.devtools.collection.MapUtils;
import com.github.paganini2008.devtools.time.TimeSlot;

/**
 * 
 * SimpleSequentialMetricCollector
 *
 * @author Fred Feng
 * @since 2.0.4
 */
public class SimpleSequentialMetricCollector<M, T extends Metric<T>> implements SequentialMetricCollector<M, T> {

	public SimpleSequentialMetricCollector(int span, TimeSlot timeSlot, int bufferSize,
			HistoricalMetricsHandler<Instant, T> historicalMetricsHandler) {
		Assert.lt(span, 1, "MetricCollector's span must greater than zero.");
		Assert.isNull(timeSlot, "MetricCollector's timeSlot must not be null.");
		Assert.lt(bufferSize, 1, "MetricCollector's bufferSize must greater than zero.");
		this.store = new ConcurrentHashMap<M, MetricCollector<Instant, T>>();
		this.supplier = () -> new SimpleMetricCollector<Instant, T>(bufferSize, historicalMetricsHandler);
		this.span = span;
		this.timeSlot = timeSlot;
	}

	private final Map<M, MetricCollector<Instant, T>> store;
	private final Supplier<MetricCollector<Instant, T>> supplier;
	private final int span;
	private final TimeSlot timeSlot;

	@Override
	public T set(M metric, Instant instant, T metricUnit, boolean merged) {
		Assert.isNull(metric, "NonNull metric");
		Assert.isNull(metricUnit, "NonNull metricUnit");
		MetricCollector<Instant, T> collector = MapUtils.get(store, metric, supplier);
		LocalDateTime ldt = timeSlot.locate(instant, span);
		return collector.set(ldt.atZone(ZoneId.systemDefault()).toInstant(), metricUnit, merged);
	}

	@Override
	public Collection<M> metrics() {
		return Collections.unmodifiableCollection(store.keySet());
	}

	@Override
	public int size() {
		return store.size();
	}

	@Override
	public void clear() {
		store.values().forEach(c -> c.clear());
		store.clear();
	}

	@Override
	public Map<Instant, T> sequence(M metric) {
		if (store.containsKey(metric)) {
			return store.get(metric).all();
		}
		return MapUtils.emptyMap();
	}

}
