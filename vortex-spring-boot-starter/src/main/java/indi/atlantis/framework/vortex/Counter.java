package indi.atlantis.framework.vortex;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.data.redis.connection.RedisConnectionFactory;

import com.github.paganini2008.devtools.date.DateUtils;
import com.github.paganini2008.devtools.multithreads.AtomicLongSequence;
import com.github.paganini2008.devtools.multithreads.Executable;
import com.github.paganini2008.devtools.multithreads.ThreadUtils;

import indi.atlantis.framework.reditools.common.RedisCounter;
import indi.atlantis.framework.seafloor.utils.BeanLifeCycle;

/**
 * 
 * Counter
 *
 * @author Jimmy Hoff
 * @version 1.0
 */
public final class Counter implements BeanLifeCycle, Executable {

	private static final String defaultRedisKeyPattern = "atlantis:framework:vortex:counter:%s:%s";

	public Counter(String name, String roleName, RedisConnectionFactory connectionFactory) {
		redisCounter = new RedisCounter(String.format(defaultRedisKeyPattern, name, roleName), connectionFactory);
	}

	private final AtomicLongSequence counter = new AtomicLongSequence(0);
	private final RedisCounter redisCounter;
	private final AtomicBoolean running = new AtomicBoolean();
	private long timestamp;

	private volatile long lastCount;
	private volatile long tps;
	private volatile long lastTotalCount;
	private volatile long totalTps;

	public void incrementCount() {
		counter.incrementAndGet();
		timestamp = System.currentTimeMillis();
	}

	public void incrementCount(long size) {
		counter.addAndGet(size);
		timestamp = System.currentTimeMillis();
	}

	public long count() {
		return counter.get();
	}

	public long totalCount() {
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

	public long tps() {
		return tps;
	}

	public long totalTps() {
		return totalTps;
	}

	public boolean isIdleTimeout(long period, TimeUnit timeUnit) {
		return (System.currentTimeMillis() - timestamp) > DateUtils.convertToMillis(period, timeUnit);
	}

	@Override
	public boolean execute() {
		long value = counter.get();
		if (value > 0) {
			long current = value;
			tps = current - lastCount;
			lastCount = current;
		}
		value = redisCounter.addAndGet(value);
		if (value > 0) {
			long current = value;
			totalTps = current - lastTotalCount;
			lastTotalCount = current;
		}
		return running.get();
	}

	public String toString() {
		StringBuilder str = new StringBuilder();
		str.append("count: ").append(count());
		str.append(", totalCount: ").append(totalCount());
		str.append(", tps: ").append(tps());
		str.append(", totalTps: ").append(totalTps());
		return str.toString();
	}

}
