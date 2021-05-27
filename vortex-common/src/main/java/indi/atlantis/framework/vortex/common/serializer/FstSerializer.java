package indi.atlantis.framework.vortex.common.serializer;

import org.nustaq.serialization.FSTConfiguration;

import indi.atlantis.framework.vortex.common.Tuple;

/**
 * 
 * FstSerializer
 * 
 * @author Fred Feng
 * 
 * 
 * @version 1.0
 */
public class FstSerializer implements Serializer {

	private final FSTConfiguration configuration = FSTConfiguration.createDefaultConfiguration();

	public byte[] serialize(Tuple tuple) {
		return configuration.asByteArray(tuple);
	}

	public Tuple deserialize(byte[] bytes) {
		Tuple tuple = (Tuple) configuration.asObject(bytes);
		tuple.setLength(bytes.length);
		return tuple;
	}

}
