package indi.atlantis.framework.vortex.aggregation;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.github.paganini2008.devtools.collection.MapUtils;

/**
 * 
 * Sequencer
 *
 * @author Jimmy Hoff
 * @version 1.0
 */
public class Sequencer<I, T extends Metric<T>> {

	private final Map<I, SequentialMetricsCollector<T>> collectors = new ConcurrentHashMap<I, SequentialMetricsCollector<T>>();
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

	public int update(I identifier, String metric, long timestamp, T metricUnit) {
		return update(identifier, metric, timestamp, metricUnit, true);
	}

	public int update(I identifier, String metric, long timestamp, T metricUnit, boolean merged) {
		SequentialMetricsCollector<T> collector = MapUtils.get(collectors, identifier, () -> {
			return new SimpleSequentialMetricsCollector<T>(bufferSize, span, spanUnit, (eldestMetric, eldestMetricUnit) -> {
				if (evictionHandler != null) {
					evictionHandler.onEldestMetricRemoval(identifier, eldestMetric, eldestMetricUnit);
				}
			});
		});
		collector.set(metric, timestamp, metricUnit, merged);
		return collector.size();
	}

	public Map<String, T> sequence(I identifier, String metric) {
		SequentialMetricsCollector<T> collector = collectors.get(identifier);
		return collector != null ? collector.sequence(metric) : MapUtils.emptyMap();
	}

	public int size(I identifier) {
		SequentialMetricsCollector<T> collector = collectors.get(identifier);
		return collector != null ? collector.size() : 0;
	}

}
