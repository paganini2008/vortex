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
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import lombok.Data;

/**
 * Keeps the {@code vortex.tsd} prefix and the {@code span}/{@code displaySize} keys of the
 * previous version, so existing configuration carries over.
 *
 * @Description: TsdStoreProperties
 * @Author: Fred Feng
 * @Date: 02/01/2025
 * @Version 2.0.0
 */
@Data
@ConfigurationProperties("vortex.tsd")
public class TsdStoreProperties {

    /** The width of one bucket in minutes. Must divide 60. */
    private int span = 1;

    /** How many buckets {@code /tsd/retrieve} returns, ending at the current one. */
    private int displaySize = 60;

    /**
     * How long a bucket lives. Each bucket key carries this as its TTL in the cluster cache,
     * so expiry needs no sweeper. Every bucket is one cache key held by every node: a series
     * at 1-minute span and 24-hour retention costs 1440 keys.
     */
    private Duration retention = Duration.ofHours(24);

    /**
     * The zone used to label and align windows when the request names none. The web UI always
     * names one: the browser's own, unless the viewer picks another.
     */
    private String defaultTimeZone = "UTC";

    /**
     * How often each node refreshes a series' last-seen time in the shared catalog. Writing it
     * on every sample would double the leader's write load for no benefit.
     */
    private Duration catalogTouchInterval = Duration.ofSeconds(30);

    /**
     * How often each node writes its copy of the data to disk, on top of the write at shutdown.
     * A node that is killed rather than stopped loses at most this much on its next start.
     * Each write briefly holds the cache's state lock, so on the leader it pauses writes for as
     * long as serialising takes. {@code 0} writes only at shutdown. Needs
     * {@code spring.spreader.multiprocessing.cache.persistent=true}.
     */
    private Duration snapshotInterval = Duration.ofMinutes(5);

    /** Origins allowed to call {@code /tsd/**} directly. The Next.js frontend proxies through
     *  its own server, so it needs none. */
    private List<String> corsAllowedOrigins = new ArrayList<>(List.of("http://localhost:3000"));

    public long spanMillis() {
        return Duration.ofMinutes(span).toMillis();
    }
}
