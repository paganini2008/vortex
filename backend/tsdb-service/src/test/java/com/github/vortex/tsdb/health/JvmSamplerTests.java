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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.health.actuate.endpoint.IndicatedHealthDescriptor;
import org.springframework.boot.health.actuate.endpoint.HealthEndpoint;
import org.springframework.boot.health.contributor.Status;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

class JvmSamplerTests {

    @Test
    @SuppressWarnings("unchecked")
    void readsMemoryThreadsGcAndHealth() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        new JvmMemoryMetrics().bindTo(registry);
        new JvmThreadMetrics().bindTo(registry);
        new ProcessorMetrics().bindTo(registry);
        Timer gc = Timer.builder("jvm.gc.pause").register(registry);
        ObjectProvider<HealthEndpoint> provider = mock(ObjectProvider.class);
        HealthEndpoint endpoint = mock(HealthEndpoint.class);
        IndicatedHealthDescriptor descriptor = mock(IndicatedHealthDescriptor.class);
        when(descriptor.getStatus()).thenReturn(Status.UP);
        when(endpoint.health()).thenReturn(descriptor);
        when(provider.getIfAvailable()).thenReturn(endpoint);

        JvmSampler sampler = new JvmSampler(registry, provider);
        gc.record(Duration.ofMillis(12));
        JvmSampler.JvmHealth first = sampler.sample();
        assertThat(first.status()).isEqualTo("UP");
        assertThat(first.heapUsed()).isPositive();
        assertThat(first.heapMax()).isGreaterThanOrEqualTo(first.heapUsed());
        assertThat(first.nonHeapUsed()).isPositive();
        assertThat(first.threads()).isPositive();
        assertThat(first.processors()).isPositive();
        assertThat(first.gcPauses()).isEqualTo(1);
        assertThat(first.gcPauseMs()).isEqualTo(12.0);

        // Garbage collection and CPU are reported since the previous reading
        long until = System.nanoTime() + 50_000_000;
        while (System.nanoTime() < until) {
            Math.sqrt(System.nanoTime());
        }
        JvmSampler.JvmHealth second = sampler.sample();
        assertThat(second.gcPauses()).isZero();
        assertThat(second.processCpu()).isBetween(0.0, 1.0).isPositive();

        when(endpoint.health()).thenReturn(null);
        assertThat(sampler.sample().status()).isEqualTo("UNKNOWN");
        when(provider.getIfAvailable()).thenReturn(null);
        assertThat(sampler.sample().status()).isEqualTo("UNKNOWN");
    }

    @Test
    @SuppressWarnings("unchecked")
    void missingMetersReadAsZero() {
        JvmSampler.JvmHealth h = new JvmSampler(new SimpleMeterRegistry(), mock(ObjectProvider.class)).sample();
        assertThat(h.heapUsed()).isZero();
        assertThat(h.processCpu()).isZero();
        assertThat(h.loadAverage()).isZero();
        assertThat(h.threads()).isZero();
    }
}
