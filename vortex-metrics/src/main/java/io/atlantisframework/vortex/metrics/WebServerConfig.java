/**
* Copyright 2017-2022 Fred Feng (paganini.fy@gmail.com)

* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package io.atlantisframework.vortex.metrics;

import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.web.embedded.undertow.UndertowServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;

import io.undertow.UndertowOptions;

/**
 * 
 * WebServerConfig
 * 
 * @author Fred Feng
 *
 * @since 2.0.1
 */
@Configuration(proxyBeanMethods = false)
public class WebServerConfig {

	@Primary
	@Bean
	public UndertowServletWebServerFactory undertowServletWebServerFactory(Environment environment, ServerProperties serverProperties) {
		UndertowServletWebServerFactory serverFactory = new UndertowServletWebServerFactory();
		final int nThreads = Runtime.getRuntime().availableProcessors();
		final int port = environment.getRequiredProperty("server.port", Integer.class);
		serverFactory.setPort(port);
		serverFactory.setIoThreads(nThreads);
		serverFactory.setWorkerThreads(nThreads << 5);
		serverFactory.setUseDirectBuffers(true);
		serverFactory.setBufferSize(8192);
		serverFactory.addBuilderCustomizers((builder) -> {
			builder.setServerOption(UndertowOptions.MAX_ENTITY_SIZE, 100L * 1024 * 1024);
		});
		return serverFactory;
	}

}
