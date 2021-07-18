/**
* Copyright 2018-2021 Fred Feng (paganini.fy@gmail.com)

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
 * @author Fred Feng
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
