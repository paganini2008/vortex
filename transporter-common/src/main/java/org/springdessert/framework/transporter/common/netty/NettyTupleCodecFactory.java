package org.springdessert.framework.transporter.common.netty;

import org.springdessert.framework.transporter.common.serializer.KryoSerializer;
import org.springdessert.framework.transporter.common.serializer.Serializer;

import io.netty.channel.ChannelHandler;

/**
 * 
 * TupleCodecFactory
 * 
 * @author Jimmy Hoff
 * @version 1.0
 */
public class NettyTupleCodecFactory implements MessageCodecFactory {

	private final Serializer serializer;

	public NettyTupleCodecFactory() {
		this(new KryoSerializer());
	}

	public NettyTupleCodecFactory(Serializer serializer) {
		this.serializer = serializer;
	}

	public ChannelHandler getEncoder() {
		return new NettyEncoderDecoders.TupleEncoder(serializer);
	}

	public ChannelHandler getDecoder() {
		return new NettyEncoderDecoders.TupleDecoder(serializer);
	}

	public Serializer getSerializer() {
		return serializer;
	}

}
