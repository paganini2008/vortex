package indi.atlantis.framework.vortex.buffer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import com.github.paganini2008.devtools.collection.ListUtils;

import indi.atlantis.framework.reditools.serializer.FstRedisSerializer;
import indi.atlantis.framework.vortex.common.Tuple;

/**
 * 
 * RedisBufferZone
 * 
 * @author Jimmy Hoff
 * @version 1.0
 */
public class RedisBufferZone implements BufferZone {

	@Value("${spring.application.cluster.name}")
	private String clusterName;

	public RedisBufferZone(RedisConnectionFactory redisConnectionFactory) {
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
	}

	private final RedisTemplate<String, Object> redisTemplate;
	private String collectionNamePrefix = DEFAULT_COLLECTION_NAME_PREFIX;

	@Override
	public void setCollectionNamePrefix(String namePrefix, String subNamePrefix) {
		this.collectionNamePrefix = namePrefix + subNamePrefix;
	}

	@Override
	public void set(String collectionName, Tuple tuple) {
		String key = collectionNamePrefix + ":" + collectionName;
		redisTemplate.opsForList().leftPush(key, tuple);
	}

	@Override
	public List<Tuple> get(String collectionName, int pullSize) {
		String key = collectionNamePrefix + ":" + collectionName;
		Tuple tuple;
		if (pullSize > 1) {
			List<Tuple> list = new ArrayList<Tuple>();
			int i = 0;
			while (null != (tuple = (Tuple) redisTemplate.opsForList().leftPop(key)) && i++ < pullSize) {
				list.add(tuple);
			}
			return list;
		} else {
			tuple = (Tuple) redisTemplate.opsForList().leftPop(key);
			return tuple != null ? Collections.singletonList(tuple) : ListUtils.emptyList();
		}
	}

	@Override
	public long size(String collectionName) {
		String key = collectionNamePrefix + ":" + collectionName;
		return redisTemplate.opsForList().size(key).intValue();
	}

}
