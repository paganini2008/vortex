package indi.atlantis.framework.vortex.sequence;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.github.paganini2008.devtools.Assert;
import com.github.paganini2008.devtools.collection.MapUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * 
 * MetricSequencer
 *
 * @author Jimmy Hoff
 * @version 1.0
 */
@Slf4j
public class MetricSequencer<I, T extends Metric<T>> {

	private final Map<I, SequentialMetricCollector<T>> collectors = new ConcurrentHashMap<I, SequentialMetricCollector<T>>();
	private int span = 1;
	private SpanUnit spanUnit = SpanUnit.MINUTE;
	private int bufferSize = 60;
	private MetricEvictionHandler<I, T> evictionHandler;

	public void setSpan(int span) {
		this.span = span;
	}

	public void setSpanUnit(SpanUnit spanUnit) {
		this.spanUnit = spanUnit;
	}

	public void setBufferSize(int bufferSize) {
		this.bufferSize = bufferSize;
	}

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

	public Collection<I> identifiers() {
		return Collections.unmodifiableCollection(collectors.keySet());
	}

	public int update(I identifier, String metric, long timestamp, T metricUnit) {
		return update(identifier, metric, timestamp, metricUnit, true);
	}

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

	public Map<String, T> sequence(I identifier, String metric) {
		SequentialMetricCollector<T> collector = collectors.get(identifier);
		return collector != null ? collector.sequence(metric) : MapUtils.emptyMap();
	}

	public int size(I identifier) {
		SequentialMetricCollector<T> collector = collectors.get(identifier);
		return collector != null ? collector.size() : 0;
	}

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
