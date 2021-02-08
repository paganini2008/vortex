package indi.atlantis.framework.vortex.common.grizzly;

import org.glassfish.grizzly.Buffer;
import org.glassfish.grizzly.Transformer;

import indi.atlantis.framework.vortex.common.Tuple;

/**
 * 
 * TupleCodecFactory
 *
 * @author Jimmy Hoff
 * @version 1.0
 */
public interface TupleCodecFactory {

	Transformer<Tuple, Buffer> getEncoder();

	Transformer<Buffer, Tuple> getDecoder();

}