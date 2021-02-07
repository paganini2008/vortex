package indi.atlantis.framework.gearless.buffer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import com.github.paganini2008.devtools.collection.MapUtils;

import indi.atlantis.framework.gearless.common.Tuple;
import indi.atlantis.framework.reditools.serializer.FstRedisSerializer;
import indi.atlantis.framework.seafloor.InstanceId;
import indi.atlantis.framework.seafloor.utils.BeanLifeCycle;
import lombok.extern.slf4j.Slf4j;

/**
 * 
 * RedisBufferZone
 * 
 * @author Jimmy Hoff
 * @version 1.0
 */
@Slf4j
public class RedisBufferZone implements BufferZone, BeanLifeCycle {

	@Autowired
	private RedisConnectionFactory redisConnectionFactory;

	@Autowired
	private InstanceId instanceId;

	@Value("${spring.application.cluster.name}")
	private String clusterName;

	@Value("${spring.application.cluster.transport.bufferzone.hashed:false}")
	private boolean hashed;

	private final Map<String, String> keyMapper = new ConcurrentHashMap<String, String>();

	private RedisTemplate<String, Object> redisTemplate;

	@Override
	public void configure() throws Exception {
		RedisTemplate<String, Object> redisTemplate = new RedisTemplate<String, Object>();
		redisTemplate.setConnectionFactory(redisConnectionFactory);
		StringRedisSerializer stringSerializer = new StringRedisSerializer();
		FstRedisSerializer<Tuple> tupleSerializer = new FstRedisSerializer<Tuple>(Tuple.class);
		redisTemplate.setKeySerializer(stringSerializer);
		redisTemplate.setValueSerializer(tupleSerializer);
		redisTemplate.setHashKeySerializer(stringSerializer);
		redisTemplate.setHashValueSerializer(tupleSerializer);
		redisTemplate.afterPropertiesSet();
		this.redisTemplate = redisTemplate;
		log.info("RedisBufferZone configure successfully.");
	}

	@Override
	public void set(String collectionName, Tuple tuple) {
		redisTemplate.opsForList().leftPush(keyFor(collectionName), tuple);
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<Tuple> get(String collectionName, int pullSize) {
		Tuple tuple;
		if (pullSize > 1) {
			List<Tuple> list = new ArrayList<Tuple>();
			int i = 0;
			while (null != (tuple = (Tuple) redisTemplate.opsForList().leftPop(keyFor(collectionName))) && i++ < pullSize) {
				list.add(tuple);
			}
			return list;
		} else {
			tuple = (Tuple) redisTemplate.opsForList().leftPop(keyFor(collectionName));
			return tuple != null ? Collections.singletonList(tuple) : Collections.EMPTY_LIST;
		}
	}

	@Override
	public long size(String collectionName) {
		return redisTemplate.opsForList().size(keyFor(collectionName)).intValue();
	}

	protected String keyFor(String collectionName) {
		return MapUtils.get(keyMapper, collectionName, () -> {
			return String.format(DEFAULT_KEY_FORMAT, clusterName, collectionName, (hashed ? ":" + instanceId.get() : ""));
		});
	}

}
