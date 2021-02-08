package indi.atlantis.framework.vortex.utils;

import java.util.Calendar;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import com.github.paganini2008.devtools.Assert;
import com.github.paganini2008.devtools.collection.MapUtils;
import com.github.paganini2008.devtools.date.DateUtils;
import com.github.paganini2008.devtools.multithreads.ThreadUtils;

/**
 * 
 * SimpleSequentialMetricsCollector
 *
 * @author Jimmy Hoff
 * @version 1.0
 */
public class SimpleSequentialMetricsCollector<T extends Metric<T>> implements SequentialMetricsCollector<T> {

	public SimpleSequentialMetricsCollector(int bufferSize, int span, SpanUnit spanUnit,
			HistoricalMetricsHandler<T> historicalMetricsHandler) {
		Assert.lt(bufferSize, 1, "MetricsCollector's bufferSize must greater than zero");
		Assert.lt(span, 1, "MetricsCollector's sequential span must greater than zero");
		this.store = new ConcurrentHashMap<String, MetricsCollector<T>>();
		this.supplier = () -> new SimpleMetricsCollector<T>(true, bufferSize, historicalMetricsHandler);
		this.span = span;
		this.spanUnit = spanUnit;
	}

	private final Map<String, MetricsCollector<T>> store;
	private final Supplier<MetricsCollector<T>> supplier;
	private final SpanUnit spanUnit;
	private final int span;
	private final ThreadLocal<Calendar> calendarLocal = ThreadUtils.newThreadLocal(() -> Calendar.getInstance());
	private String datetimePattern = DEFAULT_DATETIME_PATTERN;

	public void setDatetimePattern(String datetimePattern) {
		this.datetimePattern = datetimePattern;
	}

	@Override
	public T set(String metric, long timestamp, T metricUnit) {
		Assert.hasNoText(metric, "No metric defined");
		Assert.isNull(metricUnit, "No metricUnit inputted");
		Calendar calendar = calendarLocal.get();
		long time = spanUnit.startsInMsWith(calendar, timestamp, span);
		MetricsCollector<T> metricsCollector = MapUtils.get(store, metric, supplier);
		return metricsCollector.set(DateUtils.format(time, datetimePattern), metricUnit);
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
