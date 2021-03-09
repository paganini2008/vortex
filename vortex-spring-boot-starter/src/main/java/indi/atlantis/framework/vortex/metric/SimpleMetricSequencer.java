package indi.atlantis.framework.vortex.metric;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import com.github.paganini2008.devtools.Assert;
import com.github.paganini2008.devtools.collection.MapUtils;
import com.github.paganini2008.devtools.date.DateUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * 
 * SimpleMetricSequencer
 *
 * @author Jimmy Hoff
 * @version 1.0
 */
@Slf4j
public class SimpleMetricSequencer<I, T extends Metric<T>> implements MetricSequencer<I, T> {

	private final Map<I, SequentialMetricCollector<T>> collectors = new ConcurrentHashMap<I, SequentialMetricCollector<T>>();
	private int span = 1;
	private SpanUnit spanUnit = SpanUnit.MINUTE;
	private int bufferSize = 60;
	private MetricEvictionHandler<I, T> evictionHandler;

	@Override
	public MetricSequencer<I, T> setSpan(int span) {
		this.span = span;
		return this;
	}

	@Override
	public MetricSequencer<I, T> setSpanUnit(SpanUnit spanUnit) {
		this.spanUnit = spanUnit;
		return this;
	}

	@Override
	public MetricSequencer<I, T> setBufferSize(int bufferSize) {
		this.bufferSize = bufferSize;
		return this;
	}

	@Override
	public void setMetricEvictionHandler(MetricEvictionHandler<I, T> evictionHandler) {
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
				if (log.isTraceEnabled()) {
					log.trace("Discard metric data: {}/{}/{}", identifier, eldestMetric, eldestMetricUnit);
				}
			});
		});
		collector.set(metric, timestamp, metricUnit, merged);
		return collector.size();
	}

	@Override
	public Map<String, Map<String, Object>> sequence(String identifier, String metric, boolean asc,
			Function<Long, Map<String, Object>> render) {
		Map<String, Map<String, Object>> data = new LinkedHashMap<String, Map<String, Object>>();
		long timestamp = System.currentTimeMillis();
		SequentialMetricCollector<T> collector = collectors.get(identifier);
		Map<String, T> sequence = collector != null ? collector.sequence(metric) : MapUtils.emptyMap();
		for (Map.Entry<String, T> entry : sequence.entrySet()) {
			data.put(entry.getKey(), entry.getValue().toEntries());
			timestamp = timestamp > 0 ? Math.min(entry.getValue().getTimestamp(), timestamp) : entry.getValue().getTimestamp();
		}
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
		return DataRenderer.render(data, startTime, asc, spanUnit, span, bufferSize, render);
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
