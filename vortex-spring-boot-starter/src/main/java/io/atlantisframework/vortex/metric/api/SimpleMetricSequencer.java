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
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.github.paganini2008.devtools.Assert;
import com.github.paganini2008.devtools.collection.MapUtils;

/**
 * 
 * SimpleMetricSequencer
 *
 * @author Fred Feng
 * @since 2.0.1
 */
public class SimpleMetricSequencer<I, T extends Metric<T>> implements MetricSequencer<I, T> {

	private final int span;
	private final TimeWindowUnit timeWindowUnit;
	private final int bufferSize;
	private final MetricEvictionHandler<I, T> evictionHandler;
	private final Map<I, SequentialMetricCollector<String, T>> collectors = new ConcurrentHashMap<I, SequentialMetricCollector<String, T>>();

	public SimpleMetricSequencer(int span, TimeWindowUnit timeWindowUnit, int bufferSize, MetricEvictionHandler<I, T> evictionHandler) {
		this.span = span;
		this.timeWindowUnit = timeWindowUnit;
		this.bufferSize = bufferSize;
		this.evictionHandler = evictionHandler;
	}

	@Override
	public int getSpan() {
		return span;
	}

	@Override
	public TimeWindowUnit getTimeWindowUnit() {
		return timeWindowUnit;
	}

	@Override
	public int getBufferSize() {
		return bufferSize;
	}

	@Override
	public Collection<I> identifiers() {
		return Collections.unmodifiableCollection(collectors.keySet());
	}

	@Override
	public int trace(I identifier, String metric, long timestamp, T metricUnit, boolean merged) {
		Assert.isNull(identifier, "Undefined collector identifier");
		Assert.hasNoText(metric, "Undefined collector metric name");
		SequentialMetricCollector<String, T> collector = MapUtils.get(collectors, identifier, () -> {
			return new SimpleSequentialMetricCollector<String, T>(span, timeWindowUnit.getTimeSlot(), bufferSize,
					(instant, eldestMetricUnit) -> {
						if (evictionHandler != null) {
							evictionHandler.onEldestMetricRemoval(identifier, metric, instant, eldestMetricUnit);
						}
					});
		});
		collector.set(metric, Instant.ofEpochMilli(timestamp), metricUnit, merged);
		return collector.size();
	}

	@Override
	public Map<Instant, T> sequence(I identifier, String metric) {
		SequentialMetricCollector<String, T> collector = collectors.get(identifier);
		return collector != null ? collector.sequence(metric) : MapUtils.emptyMap();
	}

	@Override
	public int size(I identifier) {
		SequentialMetricCollector<String, T> collector = collectors.get(identifier);
		return collector != null ? collector.size() : 0;
	}

	@Override
	public void scan(ScanHandler<I, T> handler) {
		I identifier;
		SequentialMetricCollector<String, T> collector;
		for (Map.Entry<I, SequentialMetricCollector<String, T>> entry : collectors.entrySet()) {
			identifier = entry.getKey();
			collector = entry.getValue();
			for (String metric : collector.metrics()) {
				Map<Instant, T> data = collector.sequence(metric);
				handler.handleSequence(identifier, metric, data);
			}
		}
	}

}
