package org.springtribe.framework.gearless.utils;

import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

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

	HOUR {

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
		public <T> Map<String, T> newSequentialMap(int span, int bufferSize, Supplier<T> valueSupplier) {
			Map<String, T> map = new LinkedHashMap<String, T>();
			Calendar c = Calendar.getInstance();
			c.setTimeInMillis(System.currentTimeMillis());
			c.set(Calendar.MINUTE, 0);
			c.set(Calendar.SECOND, 0);
			for (int i = 0; i < bufferSize; i++) {
				map.put(DateUtils.format(c.getTime(), SimpleSequentialMetricsCollector.DEFAULT_DATETIME_PATTERN), valueSupplier.get());
				c.add(Calendar.HOUR_OF_DAY, -1 * span);
			}
			return MapUtils.reverse(map);
		}

	},
	MINUTE {

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
		public <T> Map<String, T> newSequentialMap(int span, int bufferSize, Supplier<T> valueSupplier) {
			Map<String, T> map = new LinkedHashMap<String, T>();
			Calendar c = Calendar.getInstance();
			c.setTimeInMillis(System.currentTimeMillis());
			c.set(Calendar.SECOND, 0);
			for (int i = 0; i < bufferSize; i++) {
				map.put(DateUtils.format(c.getTime(), SimpleSequentialMetricsCollector.DEFAULT_DATETIME_PATTERN), valueSupplier.get());
				c.add(Calendar.MINUTE, -1 * span);
			}
			return MapUtils.reverse(map);
		}
	},
	SECOND {

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
		public <T> Map<String, T> newSequentialMap(int span, int bufferSize, Supplier<T> valueSupplier) {
			Map<String, T> map = new LinkedHashMap<String, T>();
			Calendar c = Calendar.getInstance();
			c.setTimeInMillis(System.currentTimeMillis());
			for (int i = 0; i < bufferSize; i++) {
				map.put(DateUtils.format(c.getTime(), SimpleSequentialMetricsCollector.DEFAULT_DATETIME_PATTERN), valueSupplier.get());
				c.add(Calendar.SECOND, -1 * span);
			}
			return MapUtils.reverse(map);
		}

	};

	public abstract Calendar startsWith(Calendar c, long timestamp, int span);

	public abstract long startsInMsWith(Calendar c, long timestamp, int span);

	public abstract <T> Map<String, T> newSequentialMap(int span, int bufferSize, Supplier<T> valueSupplier);

}
