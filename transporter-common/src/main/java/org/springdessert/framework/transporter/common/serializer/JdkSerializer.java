package org.springdessert.framework.transporter.common.serializer;

import org.springdessert.framework.transporter.common.Tuple;

import com.github.paganini2008.devtools.io.SerializationUtils;

/**
 * 
 * JdkSerializer
 * 
 * @author Jimmy Hoff
 * 
 * 
 * @version 1.0
 */
public class JdkSerializer implements Serializer {

	private final boolean compress;

	public JdkSerializer() {
		this(false);
	}

	public JdkSerializer(boolean compress) {
		this.compress = compress;
	}

	public byte[] serialize(Tuple tuple) {
		return SerializationUtils.serialize(tuple, compress);
	}

	public Tuple deserialize(byte[] bytes) {
		return (Tuple) SerializationUtils.deserialize(bytes, compress);
	}

}
