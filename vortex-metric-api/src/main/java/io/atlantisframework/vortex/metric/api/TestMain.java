package io.atlantisframework.vortex.metric.api;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;

import com.github.paganini2008.devtools.RandomUtils;
import com.github.paganini2008.devtools.multithreads.ThreadUtils;
import com.github.paganini2008.devtools.time.TimeSlot;

import io.atlantisframework.vortex.metric.api.NumberMetrics.LongMetric;

public class TestMain {

	public static void main(String[] args) throws Exception {
		final int N = 100000;
		SimpleSequentialMetricCollector<String, NumberMetric<Long>> simpleSequentialMetricCollector = new SimpleSequentialMetricCollector<>(
				1, TimeSlot.MINUTE, 60, null);
		ThreadUtils.benchmark(50, 50, N, i->{
			long ms = System.currentTimeMillis();
			simpleSequentialMetricCollector.set("test", Instant.ofEpochMilli(ms), new LongMetric(RandomUtils.randomLong(1, 1000000), ms), true);
		});
		System.out.println("1");
		System.in.read();
		Map<Instant, NumberMetric<Long>> m = simpleSequentialMetricCollector.sequence("test");
		m.entrySet().forEach(e->{
			System.out.println("date: " + e.getKey());
			System.out.println(e.getValue().toEntries());
		});
	}

}
