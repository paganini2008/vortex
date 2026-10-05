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

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;

/**
 *
 * @Description: OpenApiConfig
 * @Author: Fred Feng
 * @Date: 03/10/2026
 * @Version 1.0.0
 */
@Configuration(proxyBeanMethods = false)
public class OpenApiConfig {

    @Bean
    public OpenAPI vortexOpenApi() {
        return new OpenAPI()
                .info(new Info().title("Vortex TSDB API").version("1.0.0")
                        .description("Push numeric samples and read them back as per-minute "
                                + "aggregates (count, highest, lowest, total, average). Any node "
                                + "of the cluster, or the gateway in front of them, accepts every "
                                + "call: writes are replicated to all nodes and reads are served "
                                + "from the answering node's own copy.")
                        .license(new License().name("Apache License 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")))
                // Relative, so "Try it out" works through the gateway as well as on a node
                .addServersItem(new Server().url("/"));
    }
}
