package indi.atlantis.framework.vortex.common.embeddedio;

import com.github.paganini2008.embeddedio.Serialization;

/**
 * 
 * SerializationFactory
 *
 * @author Fred Feng
 * @since 1.0
 */
public interface SerializationFactory {

	Serialization getEncoder();

	Serialization getDecoder();

}
