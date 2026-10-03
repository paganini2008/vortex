/*
 * Copyright 2017-2026 Fred Feng (paganini.fy@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.vortex.tsdb.core;

import java.time.Duration;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.stereotype.Component;
import com.chaconneai.openspreader.cache.CacheService;
import lombok.extern.slf4j.Slf4j;

/**
 * Writes this node's copy of the cache to disk every
 * {@link TsdStoreProperties#getSnapshotInterval()}.
 *
 * <p>openspreader writes the copy at shutdown and the leader loads it at the next start. A
 * node that is killed instead of stopped never gets to write, and a full cluster restart after
 * that would start empty; with this, it starts from the last snapshot instead.
 *
 * <p>Every node runs it, not one per cluster: whichever node becomes leader next time loads its
 * own file, so each needs a recent one. {@link CacheService#dumpToDisk()} itself decides when
 * another node on the same machine is the one to write.
 * 
 * @Description: CacheSnapshotTask
 * @Author: Fred Feng
 * @Date: 03/10/2026
 * @Version 2.0.0
 */
@Slf4j
@Component
public class CacheSnapshotTask implements SchedulingConfigurer {

    private final ObjectProvider<CacheService> cacheService;
    private final TsdStoreProperties properties;

    public CacheSnapshotTask(ObjectProvider<CacheService> cacheService,
            TsdStoreProperties properties) {
        this.cacheService = cacheService;
        this.properties = properties;
    }

    @Override
    public void configureTasks(ScheduledTaskRegistrar registrar) {
        Duration interval = properties.getSnapshotInterval();
        CacheService service = cacheService.getIfAvailable();
        if (service == null || interval == null || interval.isZero() || interval.isNegative()) {
            return;
        }
        registrar.addFixedDelayTask(() -> {
            // -1 when persistence is off or another node on this machine writes the file
            long bytes = service.dumpToDisk();
            if (bytes >= 0 && log.isDebugEnabled()) {
                log.debug("Snapshot written: {} KB", bytes / 1024);
            }
        }, interval);
    }
}
