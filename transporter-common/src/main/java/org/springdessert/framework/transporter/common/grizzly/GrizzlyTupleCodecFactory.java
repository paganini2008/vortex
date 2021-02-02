package org.springdessert.framework.transporter.common.grizzly;

import org.springdessert.framework.transporter.common.grizzly.GrizzlyEncoderDecoders.TupleDecoder;
import org.springdessert.framework.transporter.common.grizzly.GrizzlyEncoderDecoders.TupleEncoder;
import org.springdessert.framework.transporter.common.serializer.KryoSerializer;
import org.springdessert.framework.transporter.common.serializer.Serializer;

/**
 * 
 * GrizzlyTupleCodecFactory
 *
 * @author Jimmy Hoff
 * @version 1.0
 */
public class GrizzlyTupleCodecFactory implements TupleCodecFactory {

	private final Serializer serializer;
	
	public GrizzlyTupleCodecFactory() {
		this(new KryoSerializer());
	}

	public GrizzlyTupleCodecFactory(Serializer serializer) {
		this.serializer = serializer;
	}

	public TupleEncoder getEncoder() {
		return new TupleEncoder(serializer);
	}

	public TupleDecoder getDecoder() {
		return new TupleDecoder(serializer);
	}

	public Serializer getSerializer() {
		return serializer;
	}

}
