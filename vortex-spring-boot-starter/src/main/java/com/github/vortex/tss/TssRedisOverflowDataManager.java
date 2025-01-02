package com.github.vortex.tss;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import com.github.doodler.common.cloud.PrimaryApplicationInfoReadyEvent;
import com.github.doodler.common.cloud.SecondaryApplicationInfoRefreshEvent;
import com.github.doodler.common.timeseries.RedisOverflowDataManager;
import com.github.doodler.common.timeseries.UserMetric;
import com.github.doodler.common.utils.MapUtils;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @Description: TssRedisOverflowDataManager
 * @Author: Fred Feng
 * @Date: 02/01/2025
 * @Version 1.0.0
 */
@Slf4j
public class TssRedisOverflowDataManager<T extends UserMetric<T>>
        extends RedisOverflowDataManager<T> implements TssOverflowDataHandler<T> {

    public TssRedisOverflowDataManager(String namespace,
            RedisTemplate<String, Object> redisOperations) {
        super(namespace, redisOperations);
    }

    private final AtomicInteger control = new AtomicInteger(0);
    private final List<Backfill> backfills = new CopyOnWriteArrayList<>();

    @Override
    public void persist(String category, String dimension, Instant instant, T data) {
        if (control.get() == 1) {
            if (log.isInfoEnabled()) {
                log.info("Data will overflow to redis. Data: {}", data.represent());
            }
            super.persist(category, dimension, instant, data);
            if (backfills.size() > 0) {

                for (Backfill bf : backfills) {
                    super.persist(bf.category, bf.dimension, bf.instant, bf.data);
                    backfills.remove(bf);
                }
            }
        } else if (control.get() == 0) {
            backfills.add(new Backfill(category, dimension, instant, data));
        }
    }

    @Override
    public List<String> getDays(String category, String dimension) {
        String keyPattern =
                String.format(REDIS_KEY_PATTERN, getNamespace(), category, dimension, "*");
        Set<String> keys = getRedisOperations().keys(keyPattern);
        if (CollectionUtils.isEmpty(keys)) {
            return Collections.emptyList();
        }
        return keys.stream().map(k -> k.split(":")[4]).toList();
    }

    @Override
    public Map<Instant, Object> retrieve(String category, String dimension, int N) {
        List<String> days = getDays(category, dimension);
        if (CollectionUtils.isEmpty(days)) {
            return Collections.emptyMap();
        }
        Map<Instant, Object> results = new TreeMap<>(Comparator.reverseOrder());
        Collections.reverse(days);
        String key;
        int n = 0;
        outerLoop: for (String day : days) {
            key = String.format(REDIS_KEY_PATTERN, getNamespace(), category, dimension, day);
            Map<Object, Object> entries = getRedisOperations().opsForHash().entries(key);
            if (MapUtils.isEmpty(entries)) {
                continue;
            }
            for (Map.Entry<Object, Object> en : entries.entrySet()) {
                results.put(Instant.ofEpochMilli(Long.valueOf(en.getKey().toString())),
                        en.getValue());
                if (n++ > N) {
                    break outerLoop;
                }
            }
        }
        return Collections.unmodifiableMap(results);
    }

    @Override
    public void clean(String category) {
        String keyPattern = String.format("%s:%s:%s", getNamespace(), category, "*");
        RedisKeyIterator iterator =
                new RedisKeyIterator(keyPattern, getRedisOperations().getConnectionFactory());
        while (iterator.hasNext()) {
            String key = iterator.next();
            getRedisOperations().delete(key);
        }
    }

    @Override
    public void clean(String category, String dimension) {
        String keyPattern =
                String.format(REDIS_KEY_PATTERN, getNamespace(), category, dimension, "*");
        RedisKeyIterator iterator =
                new RedisKeyIterator(keyPattern, getRedisOperations().getConnectionFactory());
        while (iterator.hasNext()) {
            String key = iterator.next();
            getRedisOperations().delete(key);
        }
    }

    /**
     * 
     * @Description: RedisKeyIterator
     * @Author: Fred Feng
     * @Date: 02/01/2025
     * @Version 1.0.0
     */
    private static class RedisKeyIterator implements Iterator<String> {

        RedisKeyIterator(String keyPattern, RedisConnectionFactory redisConnectionFactory) {
            ScanOptions options = ScanOptions.scanOptions().match(keyPattern).build();
            RedisConnection connection = redisConnectionFactory.getConnection();
            this.cursor = connection.scan(options);
        }

        private final Cursor<byte[]> cursor;

        @Override
        public boolean hasNext() {
            return cursor.hasNext();
        }

        @Override
        public String next() {
            return new String(cursor.next(), StandardCharsets.UTF_8);
        }
    }

    @EventListener({PrimaryApplicationInfoReadyEvent.class})
    public void onPrimaryApplicationInfoReadyEvent() {
        control.set(1);
    }

    @EventListener({SecondaryApplicationInfoRefreshEvent.class})
    public void onSecondaryApplicationInfoRefreshEvent() {
        control.set(2);
        backfills.clear();
    }

    /**
     * 
     * @Description: Backfill
     * @Author: Fred Feng
     * @Date: 02/01/2025
     * @Version 1.0.0
     */
    @Getter
    @Setter
    @AllArgsConstructor
    private class Backfill {

        String category;
        String dimension;
        Instant instant;
        T data;
    }

}
