package org.springtribe.framework.gearless.buffer;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springdessert.framework.xmemcached.MemcachedTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springtribe.framework.cluster.InstanceId;
import org.springtribe.framework.gearless.common.Tuple;
import org.springtribe.framework.gearless.common.TupleImpl;

import com.github.paganini2008.devtools.collection.MapUtils;

/**
 * 
 * MemcachedBufferZone
 * 
 * @author Jimmy Hoff
 * @version 1.0
 */
public class MemcachedBufferZone implements BufferZone {

	private static final int DEFAULT_EXPIRATION = 60;

	@Value("${spring.application.cluster.name}")
	private String clusterName;

	@Value("${spring.application.cluster.transport.bufferzone.collectionName}")
	private String collectionName;

	@Value("${spring.application.cluster.transport.bufferzone.hashed:false}")
	private boolean hashed;

	@Autowired
	private InstanceId instanceId;

	@Autowired
	private MemcachedTemplate memcachedOperations;

	private final Map<String, String> keyMapper = new ConcurrentHashMap<String, String>();

	@Override
	public void set(String collectionName, Tuple tuple) throws Exception {
		memcachedOperations.push(keyFor(collectionName), DEFAULT_EXPIRATION, tuple);
	}

	@Override
	public List<Tuple> get(String collectionName, int pullSize) throws Exception {
		List<Tuple> list = new ArrayList<Tuple>();
		Tuple tuple;
		int i = 0;
		while (null != (tuple = memcachedOperations.pop(keyFor(collectionName), TupleImpl.class)) && i++ < pullSize) {
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

	protected String keyFor(String collectionName) {
		return MapUtils.get(keyMapper, collectionName, () -> {
			return String.format(DEFAULT_KEY_FORMAT, clusterName, collectionName, (hashed ? ":" + instanceId.get() : ""));
		});
	}

}
