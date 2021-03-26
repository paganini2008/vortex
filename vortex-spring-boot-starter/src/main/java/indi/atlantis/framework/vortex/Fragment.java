package indi.atlantis.framework.vortex;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import com.github.paganini2008.devtools.date.DateUtils;
import com.github.paganini2008.devtools.io.FileUtils;

/**
 * 
 * Fragment
 * 
 * @author Jimmy Hoff
 *
 * @version 1.0
 */
public class Fragment {

	final AtomicLong incrementalCount = new AtomicLong();
	final AtomicLong count = new AtomicLong();
	final AtomicLong incrementalLength = new AtomicLong();
	final AtomicLong length = new AtomicLong();
	volatile long tps;
	volatile long timestamp;

	public long getTps() {
		return tps;
	}

	public void setTps(long tps) {
		this.tps = tps;
	}

	public long getIncrementalCount() {
		long current = incrementalCount.get();
		incrementalCount.getAndAdd(-1 * current);
		return current;
	}

	public long getCount() {
		return count.get();
	}

	public long getIncrementalLength() {
		long current = incrementalLength.get();
		incrementalLength.getAndAdd(-1 * current);
		return current;
	}

	public long getLength() {
		return length.get();
	}

	public String getFormattedLength() {
		return FileUtils.formatSize(length.get());
	}

	public long getTimestamp() {
		return timestamp;
	}

	public boolean isIdleTimeout(long period, TimeUnit timeUnit) {
		return (System.currentTimeMillis() - timestamp) > DateUtils.convertToMillis(period, timeUnit);
	}

	public Map<String, Object> toEntries() {
		Map<String, Object> data = new HashMap<String, Object>();
		data.put("tps", getTps());
		data.put("count", getCount());
		data.put("length", getLength());
		data.put("timestamp", getTimestamp());
		return data;
	}

}
