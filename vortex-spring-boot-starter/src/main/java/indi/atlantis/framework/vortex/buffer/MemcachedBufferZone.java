package indi.atlantis.framework.vortex.buffer;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.github.paganini2008.springworld.xmemcached.MemcachedTemplate;

import indi.atlantis.framework.vortex.common.Tuple;
import indi.atlantis.framework.vortex.common.TupleImpl;

/**
 * 
 * MemcachedBufferZone
 * 
 * @author Fred Feng
 * @version 1.0
 */
public class MemcachedBufferZone implements BufferZone {

	private static final int DEFAULT_EXPIRATION = 60;

	private final MemcachedTemplate memcachedOperations;

	public MemcachedBufferZone(MemcachedTemplate memcachedOperations) {
		this.memcachedOperations = memcachedOperations;
	}

	private String collectionNamePrefix = DEFAULT_COLLECTION_NAME_PREFIX; 

	@Override
	public void setCollectionNamePrefix(String namePrefix, String subNamePrefix) {
		this.collectionNamePrefix = namePrefix + ":" + subNamePrefix;
	}

	@Override
	public void set(String collectionName, Tuple tuple) throws Exception {
		String key = collectionNamePrefix + ":" + collectionName;
		memcachedOperations.push(key, DEFAULT_EXPIRATION, tuple);
	}

	@Override
	public List<Tuple> get(String collectionName, int pullSize) throws Exception {
		String key = collectionNamePrefix + ":" + collectionName;
		List<Tuple> list = new ArrayList<Tuple>();
		Tuple tuple;
		int i = 0;
		while (null != (tuple = memcachedOperations.pop(key, TupleImpl.class)) && i++ < pullSize) {
			list.add(tuple);
		}
		return list;
	}

	@Override
	public long size(String collectionName) throws Exception {
		Map<InetSocketAddress, Map<String, String>> result = memcachedOperations.getClient().getStats();
		int total = 0;
		if (result != null) {
			for (Map<String, String> map : result.values()) {
				total += map.containsKey("curr_items") ? Integer.parseInt(map.get("curr_items")) : 0;
			}
		}
		return total;
	}

}
