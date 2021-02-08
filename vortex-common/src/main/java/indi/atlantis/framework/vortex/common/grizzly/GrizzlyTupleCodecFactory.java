package indi.atlantis.framework.vortex.common.grizzly;

import indi.atlantis.framework.vortex.common.grizzly.GrizzlyEncoderDecoders.TupleDecoder;
import indi.atlantis.framework.vortex.common.grizzly.GrizzlyEncoderDecoders.TupleEncoder;
import indi.atlantis.framework.vortex.common.serializer.KryoSerializer;
import indi.atlantis.framework.vortex.common.serializer.Serializer;

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
