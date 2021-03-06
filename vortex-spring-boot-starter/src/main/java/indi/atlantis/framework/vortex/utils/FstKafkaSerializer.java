package indi.atlantis.framework.vortex.utils;

import org.nustaq.serialization.FSTConfiguration;

import indi.atlantis.framework.vortex.common.Tuple;

/**
 * 
 * FstKafkaSerializer
 * 
 * @author Jimmy Hoff
 *
 * @since 1.0
 */
public class FstKafkaSerializer implements KafkaSerializer {

	private final FSTConfiguration configuration = FSTConfiguration.createDefaultConfiguration();

	@Override
	public byte[] serialize(String topic, Tuple data) {
		return configuration.asByteArray(data);
	}

	@Override
	public Tuple deserialize(String topic, byte[] data) {
		return (Tuple) configuration.asObject(data);
	}

}
