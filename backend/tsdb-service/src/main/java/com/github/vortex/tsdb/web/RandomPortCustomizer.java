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
package com.github.vortex.tsdb.web;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.IntPredicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.server.ConfigurableWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.stereotype.Component;

/**
 * With {@code server.port=0} (the default outside Docker), takes a free port in
 * {@code vortex.server.port-range} (50000-60000) rather than wherever the operating system
 * would put it, so every instance lands in one known band. An explicit {@code server.port} is
 * left alone.
 * 
 * @Description: RandomPortCustomizer
 * @Author: Fred Feng
 * @Date: 03/10/2026
 * @Version 1.0.0
 */
@Component
public class RandomPortCustomizer implements WebServerFactoryCustomizer<ConfigurableWebServerFactory> {

    static final int ATTEMPTS = 50;

    private final int configuredPort;
    private final int from;
    private final int to;
    private final IntPredicate isFree;

    @Autowired
    public RandomPortCustomizer(@Value("${server.port:0}") int configuredPort,
            @Value("${vortex.server.port-range:50000-60000}") String range) {
        this(configuredPort, range, RandomPortCustomizer::isFree);
    }

    RandomPortCustomizer(int configuredPort, String range, IntPredicate isFree) {
        String[] bounds = range.split("-");
        int lo = Integer.parseInt(bounds[0].trim());
        int hi = Integer.parseInt(bounds[1].trim());
        if (lo < 1 || hi > 65535 || lo > hi) {
            throw new IllegalArgumentException("vortex.server.port-range must look like 50000-60000, got " + range);
        }
        this.configuredPort = configuredPort;
        this.from = lo;
        this.to = hi;
        this.isFree = isFree;
    }

    @Override
    public void customize(ConfigurableWebServerFactory factory) {
        if (configuredPort == 0) {
            factory.setPort(pick());
        }
    }

    /** A free port in the range, tried at random; the operating system's choice if none is found. */
    int pick() {
        for (int i = 0; i < ATTEMPTS; i++) {
            int port = ThreadLocalRandom.current().nextInt(from, to + 1);
            if (isFree.test(port)) {
                return port;
            }
        }
        return 0;
    }

    static boolean isFree(int port) {
        try (ServerSocket s = new ServerSocket()) {
            s.setReuseAddress(false);
            s.bind(new InetSocketAddress(port));
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
