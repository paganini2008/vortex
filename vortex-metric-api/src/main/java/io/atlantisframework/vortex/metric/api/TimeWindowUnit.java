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

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

import com.github.paganini2008.devtools.collection.MapUtils;
import com.github.paganini2008.devtools.time.LocalDateTimeUtils;
import com.github.paganini2008.devtools.time.TimeSlot;

/**
 * 
 * TimeWindowUnit
 *
 * @author Fred Feng
 * @since 2.0.1
 */
public enum TimeWindowUnit {

	HOUR(TimeSlot.HOUR, Calendar.HOUR_OF_DAY) {

		@Override
		public Map<String, Map<String, Object>> descendingMap(Date startTime, int span, int bufferSize, String[] metrics,
				DateTimeFormatter df, Function<Long, Object> supplier) {
			Map<String, Map<String, Object>> data = new LinkedHashMap<String, Map<String, Object>>();
			LocalDateTime ldt = LocalDateTimeUtils.toLocalDateTime(startTime, null);
			ldt = ldt.withMinute(0).withSecond(0);
			ZoneOffset defaultOffset = OffsetDateTime.now().getOffset();
			for (int i = 0; i < bufferSize; i++) {
				Object object = supplier.apply(ldt.toInstant(defaultOffset).toEpochMilli());
				if (object != null) {
					Map<String, Object> map = new HashMap<String, Object>();
					for (String metric : metrics) {
						map.put(metric, object);
					}
					data.put(ldt.format(df), map);
				}
				ldt = ldt.plusHours(-1 * span);
			}
			return MapUtils.reverse(data);
		}

		@Override
		public Map<String, Map<String, Object>> ascendingMap(Date startTime, int span, int bufferSize, String[] metrics,
				DateTimeFormatter df, Function<Long, Object> supplier) {
			Map<String, Map<String, Object>> data = new LinkedHashMap<String, Map<String, Object>>();
			LocalDateTime ldt = LocalDateTimeUtils.toLocalDateTime(startTime, null);
			ldt = ldt.withMinute(0).withSecond(0);
			ZoneOffset defaultOffset = OffsetDateTime.now().getOffset();
			for (int i = 0; i < bufferSize; i++) {
				Object object = supplier.apply(ldt.toInstant(defaultOffset).toEpochMilli());
				if (object != null) {
					Map<String, Object> map = new HashMap<String, Object>();
					for (String metric : metrics) {
						map.put(metric, object);
					}
					data.put(ldt.format(df), map);
				}
				ldt = ldt.plusHours(span);
			}
			return data;
		}

	},
	MINUTE(TimeSlot.MINUTE, Calendar.MINUTE) {

		@Override
		public Map<String, Map<String, Object>> descendingMap(Date startTime, int span, int bufferSize, String[] metrics,
				DateTimeFormatter df, Function<Long, Object> supplier) {
			Map<String, Map<String, Object>> data = new LinkedHashMap<String, Map<String, Object>>();
			LocalDateTime ldt = LocalDateTimeUtils.toLocalDateTime(startTime, null);
			ldt = ldt.withSecond(0);
			ZoneOffset defaultOffset = OffsetDateTime.now().getOffset();
			for (int i = 0; i < bufferSize; i++) {
				Object object = supplier.apply(ldt.toInstant(defaultOffset).toEpochMilli());
				if (object != null) {
					Map<String, Object> map = new HashMap<String, Object>();
					for (String metric : metrics) {
						map.put(metric, object);
					}
					data.put(ldt.format(df), map);
				}
				ldt = ldt.plusMinutes(-1 * span);
			}
			return MapUtils.reverse(data);
		}

		@Override
		public Map<String, Map<String, Object>> ascendingMap(Date startTime, int span, int bufferSize, String[] metrics,
				DateTimeFormatter df, Function<Long, Object> supplier) {
			Map<String, Map<String, Object>> data = new LinkedHashMap<String, Map<String, Object>>();
			LocalDateTime ldt = LocalDateTimeUtils.toLocalDateTime(startTime, null);
			ldt = ldt.withSecond(0);
			ZoneOffset defaultOffset = OffsetDateTime.now().getOffset();
			for (int i = 0; i < bufferSize; i++) {
				Object object = supplier.apply(ldt.toInstant(defaultOffset).toEpochMilli());
				if (object != null) {
					Map<String, Object> map = new HashMap<String, Object>();
					for (String metric : metrics) {
						map.put(metric, object);
					}
					data.put(ldt.format(df), map);
				}
				ldt = ldt.plusMinutes(span);
			}
			return data;
		}
	},
	SECOND(TimeSlot.SECOND, Calendar.SECOND) {

		@Override
		public Map<String, Map<String, Object>> descendingMap(Date startTime, int span, int bufferSize, String[] metrics,
				DateTimeFormatter df, Function<Long, Object> supplier) {
			Map<String, Map<String, Object>> data = new LinkedHashMap<String, Map<String, Object>>();
			LocalDateTime ldt = LocalDateTimeUtils.toLocalDateTime(startTime, null);
			ZoneOffset defaultOffset = OffsetDateTime.now().getOffset();
			for (int i = 0; i < bufferSize; i++) {
				Object object = supplier.apply(ldt.toInstant(defaultOffset).toEpochMilli());
				if (object != null) {
					Map<String, Object> map = new HashMap<String, Object>();
					for (String metric : metrics) {
						map.put(metric, object);
					}
					data.put(ldt.format(df), map);
				}
				ldt = ldt.plusSeconds(-1 * span);
			}
			return MapUtils.reverse(data);
		}

		@Override
		public Map<String, Map<String, Object>> ascendingMap(Date startTime, int span, int bufferSize, String[] metrics,
				DateTimeFormatter df, Function<Long, Object> supplier) {
			Map<String, Map<String, Object>> data = new LinkedHashMap<String, Map<String, Object>>();
			LocalDateTime ldt = LocalDateTimeUtils.toLocalDateTime(startTime, null);
			ZoneOffset defaultOffset = OffsetDateTime.now().getOffset();
			for (int i = 0; i < bufferSize; i++) {
				Object object = supplier.apply(ldt.toInstant(defaultOffset).toEpochMilli());
				if (object != null) {
					Map<String, Object> map = new HashMap<String, Object>();
					for (String metric : metrics) {
						map.put(metric, object);
					}
					data.put(ldt.format(df), map);
				}
				ldt = ldt.plusSeconds(span);
			}
			return data;
		}

	};

	private TimeWindowUnit(TimeSlot timeSlot, int calendarField) {
		this.timeSlot = timeSlot;
		this.calendarField = calendarField;
	}

	private final TimeSlot timeSlot;
	private final int calendarField;

	public TimeSlot getTimeSlot() {
		return timeSlot;
	}

	public int getCalendarField() {
		return calendarField;
	}

	public abstract Map<String, Map<String, Object>> descendingMap(Date startTime, int span, int bufferSize, String[] metrics,
			DateTimeFormatter df, Function<Long, Object> supplier);

	public abstract Map<String, Map<String, Object>> ascendingMap(Date startTime, int span, int bufferSize, String[] metrics,
			DateTimeFormatter df, Function<Long, Object> supplier);

	private static final Map<Integer, TimeWindowUnit> cache = new HashMap<Integer, TimeWindowUnit>();

	static {
		for (TimeWindowUnit timeWindowUnit : TimeWindowUnit.values()) {
			cache.put(timeWindowUnit.getCalendarField(), timeWindowUnit);
		}
	}

	public static TimeWindowUnit valueOf(int calendarField) {
		if (!cache.containsKey(calendarField)) {
			throw new IllegalArgumentException("Unknown calendar field: " + calendarField);
		}
		return cache.get(calendarField);
	}

}
