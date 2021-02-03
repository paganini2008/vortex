package org.springtribe.framework.gearless;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springtribe.framework.cluster.utils.BeanLifeCycle;
import org.springtribe.framework.reditools.common.RedisCounter;

import com.github.paganini2008.devtools.multithreads.Executable;
import com.github.paganini2008.devtools.multithreads.ThreadUtils;

/**
 * 
 * Counter
 *
 * @author Jimmy Hoff
 * @version 1.0
 */
public final class Counter implements BeanLifeCycle, Executable {

	private static final String defaultRedisKeyPattern = "spring:application:transport:cluster:%s:counter:%s";

	public Counter(String name, String roleName, RedisConnectionFactory connectionFactory) {
		redisCounter = new RedisCounter(String.format(defaultRedisKeyPattern, name, roleName), connectionFactory);
	}

	private final AtomicLong counter = new AtomicLong(0);
	private final RedisCounter redisCounter;
	private final AtomicBoolean running = new AtomicBoolean();

	private volatile long increment;
	private volatile long tps;
	private volatile long totalIncrement;
	private volatile long totalTps;

	public void incrementCount() {
		counter.incrementAndGet();
	}

	public void incrementCount(int value) {
		counter.addAndGet(value);
	}

	public long get() {
		return counter.get();
	}

	public long getTotal() {
		return redisCounter.get();
	}

	public void configure() {
		counter.set(0);
		running.set(true);
		ThreadUtils.scheduleWithFixedDelay(this, 1, TimeUnit.SECONDS);
	}

	public void destroy() {
		running.set(false);
		redisCounter.destroy();
	}

	public long getTps() {
		return tps;
	}

	public long getTotalTps() {
		return totalTps;
	}

	@Override
	public boolean execute() {
		long value = counter.get();
		if (value > 0) {
			long current = value;
			tps = current - increment;
			increment = current;
		}
		value = redisCounter.addAndGet(value);
		if (value > 0) {
			long current = value;
			totalTps = current - totalIncrement;
			totalIncrement = current;
		}
		return running.get();
	}

	public String toString() {
		StringBuilder str = new StringBuilder("<Counter>");
		str.append(" count: ").append(get() + "/" + getTotal());
		str.append(", tps: ").append(getTps() + "/" + getTotalTps());
		return str.toString();
	}

}
