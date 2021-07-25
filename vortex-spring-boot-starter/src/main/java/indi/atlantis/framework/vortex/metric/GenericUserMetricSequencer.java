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

import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import com.github.paganini2008.devtools.collection.MapUtils;
import com.github.paganini2008.devtools.comparator.ComparatorHelper;
import com.github.paganini2008.devtools.date.DateUtils;

/**
 * 
 * GenericUserMetricSequencer
 * 
 * @author Fred Feng
 *
 * @version 1.0
 */
public abstract class GenericUserMetricSequencer<I, V> extends SimpleMetricSequencer<I, UserMetric<V>>
		implements UserMetricSequencer<I, V> {

	public GenericUserMetricSequencer(int span, SpanUnit spanUnit, int bufferSize,
			MetricEvictionHandler<I, UserMetric<V>> evictionHandler) {
		super(span, spanUnit, bufferSize, evictionHandler);
	}

	@Override
	public Map<String, Map<String, Object>> sequenceLatest(I identifier, String[] metrics) {
		Map<String, Map<String, Object>> renderer = new LinkedHashMap<String, Map<String, Object>>();
		for (String metric : metrics) {
			Map<String, UserMetric<V>> sequence = super.sequence(identifier, metric);
			sequence = MapUtils.sort(sequence, (left, right) -> {
				long lt = left.getValue().getTimestamp();
				long rt = right.getValue().getTimestamp();
				return ComparatorHelper.valueOf(lt - rt);
			});
			Map.Entry<String, UserMetric<V>> lastEntry = MapUtils.getLastEntry(sequence);
			if (lastEntry != null) {
				renderer.put(metric, lastEntry.getValue().toEntries());
			}
		}
		return renderer;
	}

	public Map<String, Map<String, Object>> sequence(I identifier, String[] metrics, boolean asc) {
		long timestamp = System.currentTimeMillis();
		Map<String, Map<String, Object>> renderer = new LinkedHashMap<String, Map<String, Object>>();
		String time;
		for (String metric : metrics) {
			Map<String, UserMetric<V>> sequence = super.sequence(identifier, metric);
			for (Map.Entry<String, UserMetric<V>> entry : sequence.entrySet()) {
				time = entry.getKey();
				Map<String, Object> data = MapUtils.get(renderer, time, () -> {
					Map<String, Object> map = new HashMap<String, Object>();
					map.put(metric, renderNull(entry.getValue().getTimestamp()));
					return map;
				});
				data.put(metric, render(metric, time, entry.getValue()));
				timestamp = timestamp > 0 ? Math.min(entry.getValue().getTimestamp(), timestamp) : entry.getValue().getTimestamp();
			}
		}
		return render(metrics, renderer, timestamp, asc);
	}

	protected final Map<String, Map<String, Object>> render(String[] metrics, Map<String, Map<String, Object>> renderer, long timestamp,
			boolean asc) {
		int span = getSpan();
		int bufferSize = getBufferSize();
		SpanUnit spanUnit = getSpanUnit();
		Date startTime;
		if (asc) {
			Date date = new Date(timestamp);
			int amount = span * bufferSize;
			Date endTime = DateUtils.addField(date, spanUnit.getCalendarField(), amount);
			if (endTime.compareTo(new Date()) <= 0) {
				asc = false;
				startTime = new Date();
			} else {
				startTime = date;
			}
		} else {
			startTime = new Date();
		}
		Map<String, Map<String, Object>> sequentialMap = asc ? spanUnit.ascendingMap(startTime, span, bufferSize, metrics, timeInMs -> {
			return renderNull(timeInMs);
		}) : spanUnit.descendingMap(startTime, span, bufferSize, metrics, timeInMs -> {
			return renderNull(timeInMs);
		});
		String datetime;
		for (Map.Entry<String, Map<String, Object>> entry : renderer.entrySet()) {
			datetime = entry.getKey();
			if (sequentialMap.containsKey(datetime)) {
				sequentialMap.put(datetime, entry.getValue());
			}
		}
		return sequentialMap;
	}

	protected Map<String, Object> render(String metric, String time, UserMetric<V> metricUnit) {
		return metricUnit.toEntries();
	}

	protected abstract Map<String, Object> renderNull(long timeInMs);

}
