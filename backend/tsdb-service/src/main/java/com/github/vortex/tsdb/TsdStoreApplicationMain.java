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
package com.github.vortex.tsdb;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;
import com.github.vortex.tsdb.core.TsdStoreProperties;

/**
 * Every instance started from this class joins the same openspreader cluster: on one machine
 * nothing needs configuring beyond a distinct {@code server.port}; across machines set
 * {@code spring.spreader.ip-addresses}.
 *
 * @Description: TsdStoreApplicationMain
 * @Author: Fred Feng
 * @Date: 02/01/2025
 * @Version 1.0.0
 */
@EnableScheduling
@EnableConfigurationProperties(TsdStoreProperties.class)
@SpringBootApplication
public class TsdStoreApplicationMain {

    public static void main(String[] args) {
        SpringApplication.run(TsdStoreApplicationMain.class, args);
    }
}
