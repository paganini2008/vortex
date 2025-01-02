package com.github.vortex.tss;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.RedisTemplate;
import com.github.doodler.common.cloud.PrimaryApplicationInfoReadyEvent;
import com.github.doodler.common.cloud.SecondaryApplicationInfoRefreshEvent;
import com.github.doodler.common.timeseries.RedisOverflowDataManager;
import com.github.doodler.common.timeseries.UserMetric;
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
