/**
* Copyright 2021 Fred Feng (paganini.fy@gmail.com)

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
package indi.atlantis.framework.vortex.buffer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.github.paganini2008.devtools.collection.CollectionUtils;
import com.github.paganini2008.devtools.collection.EvictionListener;
import com.github.paganini2008.devtools.collection.LruMapSupplier;
import com.github.paganini2008.devtools.collection.LruQueue;
import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;
import com.googlecode.concurrentlinkedhashmap.Weighers;

import indi.atlantis.framework.vortex.common.Tuple;
import lombok.extern.slf4j.Slf4j;

/**
 * 
 * MemoryBufferZone
 * 
 * @author Fred Feng
 * @version 1.0
 */
@Slf4j
public class MemoryBufferZone implements BufferZone {

	private final ConcurrentMap<String, LruQueue<Tuple>> cache = new ConcurrentHashMap<String, LruQueue<Tuple>>();

	private final int maxSize;

	public MemoryBufferZone() {
		this(Integer.MAX_VALUE);
	}

	public MemoryBufferZone(int maxSize) {
		this.maxSize = maxSize;
	}

	@Override
	public void set(String collectionName, Tuple tuple) {
		LruQueue<Tuple> q = cache.get(collectionName);
		if (q == null) {
			cache.putIfAbsent(collectionName, new LruQueue<Tuple>(maxSize, new ConcurrentLruMapSupplier()));
			q = cache.get(collectionName);
		}
		q.offer(tuple);
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<Tuple> get(String collectionName, int pullSize) {
		Queue<Tuple> q = cache.get(collectionName);
		Tuple tuple;
		if (pullSize > 1) {
			List<Tuple> list = new ArrayList<Tuple>();
			if (CollectionUtils.isNotEmpty(q)) {
				int i = 0;
				while (null != (tuple = q.poll()) && i++ < pullSize) {
					list.add(tuple);
				}
			}
			return list;
		} else {
			tuple = q.poll();
			return tuple != null ? Collections.singletonList(tuple) : Collections.EMPTY_LIST;
		}
	}

	@Override
	public long size(String collectionName) {
		LruQueue<Tuple> q = cache.get(collectionName);
		return q != null ? q.size() : 0;
	}

	/**
	 * 
	 * ConcurrentLruMapSupplier
	 *
	 * @author Fred Feng
	 * @version 1.0
	 */
	private static class ConcurrentLruMapSupplier implements LruMapSupplier<Integer, Tuple> {

		@Override
		public Map<Integer, Tuple> get(final int maxSize, final EvictionListener<Integer, Tuple> evictionListener) {
			if (maxSize < 1) {
				throw new IllegalArgumentException("MaxSize must greater then zero");
			}
			return new ConcurrentLinkedHashMap.Builder<Integer, Tuple>().maximumWeightedCapacity(maxSize).weigher(Weighers.singleton())
					.listener((key, value) -> {
						if (log.isWarnEnabled()) {
							log.warn("Discard tuple: {}", value.toString());
						}
						evictionListener.onEviction(key, value);
					}).build();
		}

	}

}
