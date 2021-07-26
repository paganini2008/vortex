/**
* Copyright 2017-2021 Fred Feng (paganini.fy@gmail.com)

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

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import com.github.paganini2008.devtools.collection.ListUtils;
import com.github.paganini2008.springdessert.reditools.serializer.FstRedisSerializer;

import indi.atlantis.framework.vortex.common.Tuple;

/**
 * 
 * RedisBufferZone
 * 
 * @author Fred Feng
 * @since 2.0.1
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
		this.collectionNamePrefix = namePrefix + ":" + subNamePrefix;
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
