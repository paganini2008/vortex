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
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.health.actuate.endpoint.HealthEndpoint;
import org.springframework.stereotype.Component;
import io.micrometer.core.instrument.FunctionCounter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

/**
 * This instance's JVM: memory, CPU, garbage collection, threads, and Spring Boot's own health.
 *
 * <p>Read from the meters Spring Boot already registers. Garbage collection and CPU time only
 * count up, so they are reported as the difference since the previous reading: process CPU is
 * the CPU time spent over the wall time elapsed, across all processors. (The
 * {@code process.cpu.usage} gauge read here came back as 0 in the containers while the actuator
 * showed a value, so it is not relied on.)
 *
 * @Description: JvmSampler
 * @Author: Fred Feng
 * @Date: 03/10/2026
 * @Version 2.0.0
 */
@Component
public class JvmSampler {

    /**
     * @param status         Spring Boot's aggregate health: UP, DOWN, OUT_OF_SERVICE...
     * @param processCpu     share of the CPUs available to the JVM that it used since the
     *                       previous reading, 0 to 1
     * @param loadAverage    the system's one-minute load average; 0 where not available
     * @param gcPauses       collections since the previous reading
     * @param gcPauseMs      time spent paused in them
     */
    public record JvmHealth(String status, long heapUsed, long heapCommitted, long heapMax, long nonHeapUsed,
            double processCpu, double loadAverage, int processors, long gcPauses, double gcPauseMs,
            int threads, int peakThreads) {
    }

    private final MeterRegistry registry;
    private final ObjectProvider<HealthEndpoint> health;
    private long lastGcCount;
    private double lastGcMs;
    private double lastCpuNanos = -1;
    private long lastWallNanos;

    public JvmSampler(MeterRegistry registry, ObjectProvider<HealthEndpoint> health) {
        this.registry = registry;
        this.health = health;
    }

    public synchronized JvmHealth sample() {
        long gcCount = 0;
        double gcMs = 0;
        for (Timer t : registry.find("jvm.gc.pause").timers()) {
            gcCount += t.count();
            gcMs += t.totalTime(TimeUnit.MILLISECONDS);
        }
        long gcPauses = gcCount - lastGcCount;
        double gcPauseMs = gcMs - lastGcMs;
        lastGcCount = gcCount;
        lastGcMs = gcMs;
        int processors = Runtime.getRuntime().availableProcessors();
        double processCpu = processCpu(processors);
        return new JvmHealth(status(), (long) sum("jvm.memory.used", "heap"),
                (long) sum("jvm.memory.committed", "heap"), (long) sum("jvm.memory.max", "heap"),
                (long) sum("jvm.memory.used", "nonheap"), processCpu, gauge("system.load.average.1m"),
                processors, gcPauses, Math.round(gcPauseMs * 10) / 10.0,
                (int) gauge("jvm.threads.live"), (int) gauge("jvm.threads.peak"));
    }

    /** CPU time spent since the previous reading over the wall time elapsed, all processors. */
    private double processCpu(int processors) {
        FunctionCounter cpu = registry.find("process.cpu.time").functionCounter();
        long wall = System.nanoTime();
        if (cpu == null) {
            return 0;
        }
        double nanos = cpu.count();
        double share = 0;
        if (lastCpuNanos >= 0 && wall > lastWallNanos) {
            share = (nanos - lastCpuNanos) / ((double) (wall - lastWallNanos) * processors);
        }
        lastCpuNanos = nanos;
        lastWallNanos = wall;
        return Math.round(Math.max(0, Math.min(1, share)) * 1000) / 1000.0;
    }

    private String status() {
        HealthEndpoint endpoint = health.getIfAvailable();
        if (endpoint == null) {
            return "UNKNOWN";
        }
        var h = endpoint.health();
        return h == null ? "UNKNOWN" : h.getStatus().getCode();
    }

    /** Memory meters come one per pool; the area's total is their sum. */
    private double sum(String name, String area) {
        double total = 0;
        for (Gauge g : registry.find(name).tag("area", area).gauges()) {
            double v = g.value();
            // A pool without a maximum reports -1
            if (v > 0) {
                total += v;
            }
        }
        return total;
    }

    private double gauge(String name) {
        Gauge g = registry.find(name).gauge();
        if (g == null || Double.isNaN(g.value())) {
            return 0;
        }
        return Math.round(g.value() * 1000) / 1000.0;
    }
}
