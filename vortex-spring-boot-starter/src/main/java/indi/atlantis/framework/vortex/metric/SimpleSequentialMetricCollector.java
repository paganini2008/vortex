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

import java.util.Calendar;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import com.github.paganini2008.devtools.Assert;
import com.github.paganini2008.devtools.collection.MapUtils;
import com.github.paganini2008.devtools.date.DateUtils;
import com.github.paganini2008.devtools.multithreads.ThreadUtils;

import indi.atlantis.framework.vortex.utils.HistoricalMetricsHandler;

/**
 * 
 * SimpleSequentialMetricCollector
 *
 * @author Fred Feng
 * @version 1.0
 */
public class SimpleSequentialMetricCollector<T extends Metric<T>> implements SequentialMetricCollector<T> {

	public SimpleSequentialMetricCollector(int bufferSize, int span, SpanUnit spanUnit,
			HistoricalMetricsHandler<T> historicalMetricsHandler) {
		Assert.lt(bufferSize, 1, "MetricCollector's bufferSize must greater than zero.");
		Assert.lt(span, 1, "MetricCollector's sequential span must greater than zero.");
		if (spanUnit == SpanUnit.SECOND) {
			Assert.isTrue((span % 10) != 0, "MetricCollector's sequential span must be divided by 10 exactly when spanUnit is second.");
		}
		this.store = new ConcurrentHashMap<String, MetricCollector<T>>();
		this.supplier = () -> new SimpleMetricCollector<T>(bufferSize, true, historicalMetricsHandler);
		this.span = span;
		this.spanUnit = spanUnit;
	}

	private final Map<String, MetricCollector<T>> store;
	private final Supplier<MetricCollector<T>> supplier;
	private final SpanUnit spanUnit;
	private final int span;
	private final ThreadLocal<Calendar> calendarLocal = ThreadUtils.newThreadLocal(() -> Calendar.getInstance());
	private String datetimePattern = DEFAULT_DATETIME_PATTERN;

	public void setDatetimePattern(String datetimePattern) {
		this.datetimePattern = datetimePattern;
	}

	@Override
	public T set(String metric, long timestamp, T metricUnit, boolean merged) {
		Assert.hasNoText(metric, "No metric defined");
		Assert.isNull(metricUnit, "No metric unit inputted");
		Calendar calendar = calendarLocal.get();
		long time = spanUnit.startsInMsWith(calendar, timestamp, span);
		MetricCollector<T> metricsCollector = MapUtils.get(store, metric, supplier);
		return metricsCollector.set(DateUtils.format(time, datetimePattern), metricUnit, merged);
	}

	@Override
	public String[] metrics() {
		return store.keySet().toArray(new String[0]);
	}

	@Override
	public Map<String, T> sequence(String metric) {
		if (store.containsKey(metric)) {
			return store.get(metric).all();
		}
		return MapUtils.emptyMap();
	}

	@Override
	public int size() {
		return store.size();
	}

	@Override
	public void clear() {
		store.values().forEach(c -> c.clear());
	}

}
