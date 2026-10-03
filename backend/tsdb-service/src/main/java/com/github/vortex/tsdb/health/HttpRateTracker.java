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

import java.util.concurrent.TimeUnit;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

/**
 * This instance's API traffic over the last few seconds: requests per second, the share that
 * failed, and the mean latency.
 *
 * <p>Micrometer's {@code http.server.requests} only counts up, so the rates come from the
 * difference between two readings {@link #WINDOW_MS} apart. Only {@code /tsd/**} is counted,
 * and not the health endpoints themselves, which a dashboard polls continually.
 *
 * @Description: HttpRateTracker
 * @Author: Fred Feng
 * @Date: 03/10/2026
 * @Version 2.0.0
 */
@Component
public class HttpRateTracker {

    static final long WINDOW_MS = 5_000;

    /** Rates over the last window, and totals since the instance started. */
    public record HttpRates(double qps, double errorRate, double serverErrorRate, double avgLatencyMs,
            long requests, long clientErrors, long serverErrors) {

        static final HttpRates NONE = new HttpRates(0, 0, 0, 0, 0, 0, 0);
    }

    record Totals(long requests, long clientErrors, long serverErrors, double timeMs, long at) {
    }

    private final MeterRegistry registry;
    private volatile Totals previous;
    private volatile HttpRates current = HttpRates.NONE;

    public HttpRateTracker(MeterRegistry registry) {
        this.registry = registry;
    }

    public HttpRates current() {
        return current;
    }

    @Scheduled(fixedRate = WINDOW_MS)
    public void sample() {
        Totals now = read(System.currentTimeMillis());
        Totals before = previous;
        previous = now;
        if (before != null) {
            current = rates(before, now);
        }
    }

    Totals read(long at) {
        long requests = 0, clientErrors = 0, serverErrors = 0;
        double timeMs = 0;
        for (Timer t : registry.find("http.server.requests").timers()) {
            String uri = t.getId().getTag("uri");
            if (uri == null || !uri.startsWith("/tsd/") || uri.startsWith("/tsd/health")) {
                continue;
            }
            long count = t.count();
            requests += count;
            timeMs += t.totalTime(TimeUnit.MILLISECONDS);
            String outcome = t.getId().getTag("outcome");
            if ("CLIENT_ERROR".equals(outcome)) {
                clientErrors += count;
            } else if ("SERVER_ERROR".equals(outcome)) {
                serverErrors += count;
            }
        }
        return new Totals(requests, clientErrors, serverErrors, timeMs, at);
    }

    static HttpRates rates(Totals before, Totals now) {
        long requests = now.requests() - before.requests();
        double seconds = Math.max(1, now.at() - before.at()) / 1000.0;
        double errors = (now.clientErrors() - before.clientErrors()) + (now.serverErrors() - before.serverErrors());
        double serverErrors = now.serverErrors() - before.serverErrors();
        return new HttpRates(round(requests / seconds),
                requests > 0 ? round(errors / requests) : 0,
                requests > 0 ? round(serverErrors / requests) : 0,
                requests > 0 ? round((now.timeMs() - before.timeMs()) / requests) : 0,
                now.requests(), now.clientErrors(), now.serverErrors());
    }

    private static double round(double v) {
        return Math.round(v * 1000) / 1000.0;
    }
}
