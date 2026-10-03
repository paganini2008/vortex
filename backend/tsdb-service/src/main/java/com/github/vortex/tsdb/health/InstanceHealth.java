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
package com.github.vortex.tsdb.health;

import java.util.Map;

/**
 * One instance's health, as it reports it.
 *
 * @param reportedAt when the instance took this reading, epoch ms
 * @param reachable  false when the instance has not reported recently; every other field but
 *                   the identity is then empty
 * @param spreader  openspreader's own figures, as its actuator endpoint reports them: the
 *                  cluster (split brain), each channel's throughput, latency and errors, and its
 *                  components (cache, mutex, scheduled tasks)
 * @param http      this instance's own API traffic
 * @param jvm       memory, CPU, garbage collection and threads, and Spring Boot's own health
 *
 * @Description: InstanceHealth
 * @Author: Fred Feng
 * @Date: 03/10/2026
 * @Version 2.0.0
 */
public record InstanceHealth(String id, String host, String serverPort, String state, long startTime,
        boolean leader, long reportedAt, boolean reachable, String error, Map<String, Object> spreader,
        HttpRateTracker.HttpRates http, JvmSampler.JvmHealth jvm) {

    /** The cache's applied version, read from {@link #spreader}; 0 when absent. */
    public long appliedVersion() {
        if (spreader != null && spreader.get("components") instanceof Map<?, ?> components
                && components.get("cache") instanceof Map<?, ?> cache
                && cache.get("appliedVersion") instanceof Number n) {
            return n.longValue();
        }
        return 0;
    }

    public static InstanceHealth unreachable(String id, String host, String serverPort, String state,
            long startTime, boolean leader, long reportedAt, String error) {
        return new InstanceHealth(id, host, serverPort, state, startTime, leader, reportedAt, false, error,
                Map.of(), null, null);
    }
}
