package org.springtribe.framework.gearless.common.embeddedio;

import org.springtribe.framework.gearless.common.Tuple;
import org.springtribe.framework.gearless.common.serializer.KryoSerializer;
import org.springtribe.framework.gearless.common.serializer.Serializer;

import com.github.paganini2008.embeddedio.Serialization;

/**
 * 
 * EmbeddedSerializationFactory
 *
 * @author Jimmy Hoff
 * @since 1.0
 */
public class EmbeddedSerializationFactory implements SerializationFactory {

	private final Serializer serializer;

	public EmbeddedSerializationFactory() {
		this(new KryoSerializer());
	}

	public EmbeddedSerializationFactory(Serializer serializer) {
		this.serializer = serializer;
	}

	@Override
	public Serialization getEncoder() {
		return new Serialization() {

			@Override
			public byte[] serialize(Object object) {
				return serializer.serialize((Tuple) object);
			}

			@Override
			public Tuple deserialize(byte[] bytes) {
				return (Tuple) serializer.deserialize(bytes);
			}
		};
	}

	@Override
	public Serialization getDecoder() {
		return getEncoder();
	}

}
