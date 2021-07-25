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
 * @version 1.0
 */
public class SimpleMetricSequencer<I, T extends Metric<T>> implements MetricSequencer<I, T> {

	private final int span;
	private final SpanUnit spanUnit;
	private final int bufferSize;
	private final MetricEvictionHandler<I, T> evictionHandler;
	private final Map<I, SequentialMetricCollector<T>> collectors = new ConcurrentHashMap<I, SequentialMetricCollector<T>>();

	public SimpleMetricSequencer(int span, SpanUnit spanUnit, int bufferSize, MetricEvictionHandler<I, T> evictionHandler) {
		this.span = span;
		this.spanUnit = spanUnit;
		this.bufferSize = bufferSize;
		this.evictionHandler = evictionHandler;
	}

	public int getSpan() {
		return span;
	}

	public SpanUnit getSpanUnit() {
		return spanUnit;
	}

	public int getBufferSize() {
		return bufferSize;
	}

	@Override
	public Collection<I> identifiers() {
		return Collections.unmodifiableCollection(collectors.keySet());
	}

	@Override
	public int update(I identifier, String metric, long timestamp, T metricUnit, boolean merged) {
		Assert.isNull(identifier, "Undefined collector identifier");
		Assert.hasNoText(metric, "Undefined collector metric name");
		SequentialMetricCollector<T> collector = MapUtils.get(collectors, identifier, () -> {
			return new SimpleSequentialMetricCollector<T>(bufferSize, span, spanUnit, (eldestMetric, eldestMetricUnit) -> {
				if (evictionHandler != null) {
					evictionHandler.onEldestMetricRemoval(identifier, eldestMetric, eldestMetricUnit);
				}
			});
		});
		collector.set(metric, timestamp, metricUnit, merged);
		return collector.size();
	}

	@Override
	public Map<String, T> sequence(I identifier, String metric) {
		SequentialMetricCollector<T> collector = collectors.get(identifier);
		return collector != null ? collector.sequence(metric) : MapUtils.emptyMap();
	}

	@Override
	public int size(I identifier) {
		SequentialMetricCollector<T> collector = collectors.get(identifier);
		return collector != null ? collector.size() : 0;
	}

	@Override
	public void scan(ScanHandler<I, T> handler) {
		I identifier;
		SequentialMetricCollector<T> collector;
		for (Map.Entry<I, SequentialMetricCollector<T>> entry : collectors.entrySet()) {
			identifier = entry.getKey();
			collector = entry.getValue();
			for (String metric : collector.metrics()) {
				Map<String, T> data = collector.sequence(metric);
				handler.handleSequence(identifier, metric, data);
			}
		}
	}

}
