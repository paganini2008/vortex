package indi.atlantis.framework.vortex.metric;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Import;

import indi.atlantis.framework.tridenter.EnableApplicationCluster;
import indi.atlantis.framework.vortex.EnableNioTransport;

/**
 * 
 * EnableMetricSequencerServer
 *
 * @author Fred Feng
 * @version 1.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@EnableNioTransport
@EnableApplicationCluster(enableLeaderElection = true, enableMonitor = true)
@Import({ MetricSequencerAutoConfiguration.class })
public @interface EnableMetricSequencerServer {
}
