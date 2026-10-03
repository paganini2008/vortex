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

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import com.chaconneai.openspreader.scheduling.MultiProcessingScheduled;
import lombok.extern.slf4j.Slf4j;

/**
 * Removes expired series from the catalog and renews the instant value hashes of live
 * categories. Bucket keys expire through their own TTL; the catalog and the hashes are shared
 * by many series, so they are maintained here instead.
 *
 * <p>{@link MultiProcessingScheduled} makes one instance per round run it, whatever the
 * replica count.
 *
 * @Description: CatalogPruneTask
 * @Author: Fred Feng
 * @Date: 03/10/2026
 * @Version 2.0.0
 */
@Slf4j
@Component
public class CatalogPruneTask {

    private final TsdStoreService tsdStoreService;

    public CatalogPruneTask(TsdStoreService tsdStoreService) {
        this.tsdStoreService = tsdStoreService;
    }

    @Scheduled(initialDelay = 60_000, fixedDelay = 60_000)
    @MultiProcessingScheduled("vortex-tsd-catalog-prune")
    public void prune() {
        int removed = tsdStoreService.pruneCatalog();
        if (removed > 0) {
            log.info("Pruned {} expired series from the catalog", removed);
        }
    }
}
