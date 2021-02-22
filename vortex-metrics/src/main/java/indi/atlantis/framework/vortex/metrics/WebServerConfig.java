package indi.atlantis.framework.vortex.metrics;

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
 * @author Jimmy Hoff
 *
 * @version 1.0
 */
@Configuration
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
