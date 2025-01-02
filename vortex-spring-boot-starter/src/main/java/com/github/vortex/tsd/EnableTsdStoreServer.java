package com.github.vortex.tsd;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Import;
import com.github.doodler.common.transmitter.EnableNioTransmitter;

/**
 * 
 * @Description: EnableTsdStoreServer
 * @Author: Fred Feng
 * @Date: 02/01/2025
 * @Version 1.0.0
 */
@Retention(value = RetentionPolicy.RUNTIME)
@Target(value = {ElementType.TYPE})
@Documented
@EnableDiscoveryClient
@EnableNioTransmitter
@Import({TsdStoreAutoConfiguration.class})
public @interface EnableTsdStoreServer {
}
