package org.springdessert.framework.transporter.common.grizzly;

import org.glassfish.grizzly.Buffer;
import org.glassfish.grizzly.Transformer;
import org.springdessert.framework.transporter.common.Tuple;

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