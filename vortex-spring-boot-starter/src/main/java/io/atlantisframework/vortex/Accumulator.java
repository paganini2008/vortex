/**
* Copyright 2017-2022 Fred Feng (paganini.fy@gmail.com)

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
package io.atlantisframework.vortex;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import com.github.paganini2008.devtools.collection.MapUtils;
import com.github.paganini2008.devtools.multithreads.Executable;
import com.github.paganini2008.devtools.multithreads.ThreadUtils;
import com.github.paganini2008.springdessert.reditools.common.RedisCalulation;

import io.atlantisframework.tridenter.utils.BeanLifeCycle;
import io.atlantisframework.vortex.common.Tuple;

/**
 * 
 * Accumulator
 * 
 * @author Fred Feng
 *
 * @since 2.0.1
 */
public class Accumulator extends Fragment implements Executable, BeanLifeCycle {

	private final Map<String, Fragment> fragments = new ConcurrentHashMap<String, Fragment>();
	private final Summary summary = new Summary();
	private final String identifier;
	private final RedisCalulation redisCalulation;
	private Timer timer;

	public Accumulator(String identifier, RedisCalulation redisCalulation) {
		this.identifier = identifier;
		this.redisCalulation = redisCalulation;
	}

	public void accumulate(List<Tuple> tuples) {
		tuples.forEach(tuple -> accumulate(tuple));
	}

	public void accumulate(Tuple tuple) {
		String topic = tuple.getTopic();
		Fragment frag = MapUtils.get(fragments, topic, () -> new Fragment());
		frag.incrementalCount.incrementAndGet();
		frag.count.incrementAndGet();
		frag.incrementalLength.addAndGet(tuple.getLength());
		frag.length.addAndGet(tuple.getLength());
		frag.timestamp = tuple.getTimestamp();

		this.incrementalCount.incrementAndGet();
		this.count.incrementAndGet();
		this.incrementalLength.addAndGet(tuple.getLength());
		this.length.addAndGet(tuple.getLength());
		this.timestamp = tuple.getTimestamp();
	}

	public Map<String, Map<String, Object>> summaries() {
		Map<String, Map<String, Object>> detail = new HashMap<String, Map<String, Object>>();
		detail.put("self", summary.toEntries());
		for (Map.Entry<String, Summary> entry : summary.getChildren().entrySet()) {
			detail.put(entry.getKey(), entry.getValue().toEntries());
		}
		return detail;
	}

	public Map<String, Map<String, Object>> fragments() {
		Map<String, Map<String, Object>> detail = new HashMap<String, Map<String, Object>>();
		detail.put("self", toEntries());
		for (Map.Entry<String, Fragment> entry : fragments.entrySet()) {
			detail.put(entry.getKey(), entry.getValue().toEntries());
		}
		return detail;
	}

	@Override
	public boolean execute() {
		long incrementalCount = getIncrementalCount();
		setTps(incrementalCount);

		String key = identifier + ":count";
		long currentCount = redisCalulation.getLong(key);
		summary.setCount(redisCalulation.addAndGetLong(key, incrementalCount));
		summary.setTps(summary.getCount() - currentCount);
		key = identifier + ":length";
		summary.setLength(redisCalulation.addAndGetLong(key, getIncrementalLength()));
		summary.setTimestamp(timestamp);

		String topic;
		Fragment fragment;
		Summary children;
		for (Map.Entry<String, Fragment> entry : fragments.entrySet()) {
			topic = entry.getKey();
			fragment = entry.getValue();
			children = MapUtils.get(summary.getChildren(), topic, () -> new Summary());
			incrementalCount = fragment.getIncrementalCount();
			fragment.setTps(incrementalCount);

			key = identifier + ":" + topic + ":count";
			currentCount = redisCalulation.getLong(key);
			children.setCount(redisCalulation.addAndGetLong(key, incrementalCount));
			children.setTps(children.getCount() - currentCount);
			key = identifier + ":" + topic + ":length";
			children.setLength(redisCalulation.addAndGetLong(key, fragment.getIncrementalLength()));
			children.setTimestamp(fragment.getTimestamp());
		}
		return true;
	}

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

}
