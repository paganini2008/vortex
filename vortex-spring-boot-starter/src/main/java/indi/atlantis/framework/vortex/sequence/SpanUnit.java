package indi.atlantis.framework.vortex.sequence;

import static indi.atlantis.framework.vortex.sequence.SequentialMetricCollector.DEFAULT_DATETIME_PATTERN;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import com.github.paganini2008.devtools.collection.MapUtils;
import com.github.paganini2008.devtools.date.DateUtils;

/**
 * 
 * SpanUnit
 *
 * @author Jimmy Hoff
 * @version 1.0
 */
public enum SpanUnit {

	HOUR(Calendar.HOUR_OF_DAY) {

		@Override
		public Calendar startsWith(Calendar c, long timestamp, int span) {
			c.setTimeInMillis(timestamp);
			int hour = c.get(Calendar.HOUR_OF_DAY);
			c.set(Calendar.HOUR_OF_DAY, hour - hour % span);
			return c;
		}

		@Override
		public long startsInMsWith(Calendar c, long timestamp, int span) {
			c = startsWith(c, timestamp, span);
			return TimeUnit.HOURS.convert(c.getTimeInMillis(), TimeUnit.MILLISECONDS) * 60 * 60 * 1000;
		}

		@Override
		public <T> Map<String, T> descendingMap(Date startTime, int span, int bufferSize, Function<Long, T> f) {
			Map<String, T> map = new LinkedHashMap<String, T>();
			Calendar c = Calendar.getInstance();
			c.setTime(startTime);
			c.set(Calendar.MINUTE, 0);
			c.set(Calendar.SECOND, 0);
			for (int i = 0; i < bufferSize; i++) {
				map.put(DateUtils.format(c.getTime(), DEFAULT_DATETIME_PATTERN), f.apply(c.getTimeInMillis()));
				c.add(Calendar.HOUR_OF_DAY, -1 * span);
			}
			return MapUtils.reverse(map);
		}

		@Override
		public <T> Map<String, T> ascendingMap(Date startTime, int span, int bufferSize, Function<Long, T> f) {
			Map<String, T> map = new LinkedHashMap<String, T>();
			Calendar c = Calendar.getInstance();
			c.setTime(startTime);
			c.set(Calendar.MINUTE, 0);
			c.set(Calendar.SECOND, 0);
			for (int i = 0; i < bufferSize; i++) {
				map.put(DateUtils.format(c.getTime(), DEFAULT_DATETIME_PATTERN), f.apply(c.getTimeInMillis()));
				c.add(Calendar.HOUR_OF_DAY, span);
			}
			return map;
		}

	},
	MINUTE(Calendar.MINUTE) {

		@Override
		public Calendar startsWith(Calendar c, long timestamp, int span) {
			c.setTimeInMillis(timestamp);
			int minute = c.get(Calendar.MINUTE);
			c.set(Calendar.MINUTE, minute - minute % span);
			return c;
		}

		@Override
		public long startsInMsWith(Calendar c, long timestamp, int span) {
			c = startsWith(c, timestamp, span);
			return TimeUnit.MINUTES.convert(c.getTimeInMillis(), TimeUnit.MILLISECONDS) * 60 * 1000;
		}

		@Override
		public <T> Map<String, T> descendingMap(Date startTime, int span, int bufferSize, Function<Long, T> f) {
			Map<String, T> map = new LinkedHashMap<String, T>();
			Calendar c = Calendar.getInstance();
			c.setTime(startTime);
			c.set(Calendar.SECOND, 0);
			for (int i = 0; i < bufferSize; i++) {
				map.put(DateUtils.format(c.getTime(), DEFAULT_DATETIME_PATTERN), f.apply(c.getTimeInMillis()));
				c.add(Calendar.MINUTE, -1 * span);
			}
			return MapUtils.reverse(map);
		}

		@Override
		public <T> Map<String, T> ascendingMap(Date startTime, int span, int bufferSize, Function<Long, T> f) {
			Map<String, T> map = new LinkedHashMap<String, T>();
			Calendar c = Calendar.getInstance();
			c.setTime(startTime);
			c.set(Calendar.SECOND, 0);
			for (int i = 0; i < bufferSize; i++) {
				map.put(DateUtils.format(c.getTime(), DEFAULT_DATETIME_PATTERN), f.apply(c.getTimeInMillis()));
				c.add(Calendar.MINUTE, span);
			}
			return map;
		}
	},
	SECOND(Calendar.SECOND) {

		@Override
		public Calendar startsWith(Calendar c, long timestamp, int span) {
			c.setTimeInMillis(timestamp);
			int second = c.get(Calendar.SECOND);
			c.set(Calendar.SECOND, second - second % span);
			return c;
		}

		@Override
		public long startsInMsWith(Calendar c, long timestamp, int span) {
			c = startsWith(c, timestamp, span);
			return TimeUnit.SECONDS.convert(c.getTimeInMillis(), TimeUnit.MILLISECONDS) * 1000;
		}

		@Override
		public <T> Map<String, T> descendingMap(Date startTime, int span, int bufferSize, Function<Long, T> f) {
			Map<String, T> map = new LinkedHashMap<String, T>();
			Calendar c = Calendar.getInstance();
			c.setTime(startTime);
			for (int i = 0; i < bufferSize; i++) {
				map.put(DateUtils.format(c.getTimeInMillis(), DEFAULT_DATETIME_PATTERN), f.apply(c.getTimeInMillis()));
				c.add(Calendar.SECOND, -1 * span);
			}
			return MapUtils.reverse(map);
		}

		@Override
		public <T> Map<String, T> ascendingMap(Date startTime, int span, int bufferSize, Function<Long, T> f) {
			Map<String, T> map = new LinkedHashMap<String, T>();
			Calendar c = Calendar.getInstance();
			c.setTime(startTime);
			for (int i = 0; i < bufferSize; i++) {
				map.put(DateUtils.format(c.getTimeInMillis(), DEFAULT_DATETIME_PATTERN), f.apply(c.getTimeInMillis()));
				c.add(Calendar.SECOND, span);
			}
			return map;
		}

	};

	private SpanUnit(int calendarField) {
		this.calendarField = calendarField;
	}

	private final int calendarField;

	public int getCalendarField() {
		return calendarField;
	}

	public abstract Calendar startsWith(Calendar c, long timestamp, int span);

	public abstract long startsInMsWith(Calendar c, long timestamp, int span);

	public abstract <T> Map<String, T> descendingMap(Date startTime, int span, int bufferSize, Function<Long, T> f);

	public abstract <T> Map<String, T> ascendingMap(Date startTime, int span, int bufferSize, Function<Long, T> f);

	private static final Map<Integer, SpanUnit> cache = new HashMap<Integer, SpanUnit>();

	static {
		for (SpanUnit spanUnit : SpanUnit.values()) {
			cache.put(spanUnit.getCalendarField(), spanUnit);
		}
	}

	public static SpanUnit valueOf(int calendarField) {
		if (!cache.containsKey(calendarField)) {
			throw new IllegalArgumentException("Unknown calendar field: " + calendarField);
		}
		return cache.get(calendarField);
	}

}
