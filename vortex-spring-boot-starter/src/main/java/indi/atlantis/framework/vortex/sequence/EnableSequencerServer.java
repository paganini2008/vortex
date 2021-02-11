package indi.atlantis.framework.vortex.sequence;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Import;

import indi.atlantis.framework.vortex.EnableEmbeddedServer;

/**
 * 
 * EnableSequencerServer
 *
 * @author Jimmy Hoff
 * @version 1.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@EnableEmbeddedServer
@Import(SequencerAutoConfiguration.class)
public @interface EnableSequencerServer {
}
