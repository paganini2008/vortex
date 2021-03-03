package indi.atlantis.framework.vortex;

import java.util.List;
import java.util.Timer;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import com.github.paganini2008.devtools.date.DateUtils;
import com.github.paganini2008.devtools.io.FileUtils;
import com.github.paganini2008.devtools.multithreads.Executable;
import com.github.paganini2008.devtools.multithreads.ThreadUtils;

import indi.atlantis.framework.tridenter.utils.BeanLifeCycle;
import indi.atlantis.framework.vortex.common.Tuple;

/**
 * 
 * Accumulator
 * 
 * @author Jimmy Hoff
 *
 * @version 1.0
 */
public class Accumulator implements Executable, BeanLifeCycle {

	private final AtomicInteger tps = new AtomicInteger();
	private final AtomicLong count = new AtomicLong();
	private final AtomicLong length = new AtomicLong();
	private volatile String content;
	private volatile int tpsValue;
	private Timer timer;
	private long timestamp;

	@Override
	public void configure() throws Exception {
		this.timer = ThreadUtils.scheduleWithFixedDelay(this, 1, TimeUnit.SECONDS);
	}

	@Override
	public void destroy() {
		if (timer != null) {
			timer.cancel();
		}
	}

	void calculateTps() {
		final int current = tps.get();
		this.tpsValue = current;
		tps.getAndAdd(-1 * current);
	}

	public long getCount() {
		return count.get();
	}

	public long getLength() {
		return length.get();
	}

	public String getLengthString() {
		return FileUtils.formatSize(length.get());
	}

	public int getTps() {
		return tpsValue;
	}

	public String getContent() {
		return content;
	}

	public void accumulate(List<Tuple> tuples) {
		tuples.forEach(tuple -> accumulate(tuple));
	}

	public void accumulate(Tuple tuple) {
		this.tps.incrementAndGet();
		this.count.incrementAndGet();
		this.length.addAndGet(tuple.getLength());
		this.content = tuple.getContent();
		this.timestamp = System.currentTimeMillis();
	}

	public long getTimestamp() {
		return timestamp;
	}

	public boolean isIdleTimeout(long period, TimeUnit timeUnit) {
		return (System.currentTimeMillis() - timestamp) > DateUtils.convertToMillis(period, timeUnit);
	}

	@Override
	public boolean execute() {
		calculateTps();
		return true;
	}

	public String toString() {
		StringBuilder str = new StringBuilder();
		str.append("[Accumulator] count: ").append(getCount());
		str.append(", length: ").append(getLength());
		str.append(", tps: ").append(getTps());
		str.append(", active: ").append(!isIdleTimeout(1, TimeUnit.MINUTES));
		return str.toString();
	}

}
