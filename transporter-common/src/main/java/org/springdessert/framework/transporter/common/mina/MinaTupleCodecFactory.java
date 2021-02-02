package org.springdessert.framework.transporter.common.mina;

import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFactory;
import org.apache.mina.filter.codec.ProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolEncoder;
import org.springdessert.framework.transporter.common.mina.MinaEncoderDecoders.TupleDecoder;
import org.springdessert.framework.transporter.common.mina.MinaEncoderDecoders.TupleEncoder;
import org.springdessert.framework.transporter.common.serializer.KryoSerializer;
import org.springdessert.framework.transporter.common.serializer.Serializer;

/**
 * 
 * MinaSerializationCodecFactory
 * 
 * @author Jimmy Hoff
 * @version 1.0
 */
public class MinaTupleCodecFactory implements ProtocolCodecFactory {

	private final TupleEncoder encoder;
	private final TupleDecoder decoder;

	public MinaTupleCodecFactory() {
		this(new KryoSerializer());
	}

	public MinaTupleCodecFactory(Serializer serializer) {
		encoder = new TupleEncoder(serializer);
		decoder = new TupleDecoder(serializer);
	}

	public ProtocolEncoder getEncoder(IoSession session) {
		return encoder;
	}

	public ProtocolDecoder getDecoder(IoSession session) {
		return decoder;
	}

	public int getEncoderMaxObjectSize() {
		return encoder.getMaxObjectSize();
	}

	public void setEncoderMaxObjectSize(int maxObjectSize) {
		encoder.setMaxObjectSize(maxObjectSize);
	}

	public int getDecoderMaxObjectSize() {
		return decoder.getMaxObjectSize();
	}

	public void setDecoderMaxObjectSize(int maxObjectSize) {
		decoder.setMaxObjectSize(maxObjectSize);
	}
	
	
}
