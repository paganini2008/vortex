package org.springdessert.framework.transporter.utils;

import java.util.Map;

import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serializer;
import org.springdessert.framework.transporter.common.Tuple;

/**
 * 
 * KafkaSerializer
 * 
 * @author Jimmy Hoff
 *
 * @since 1.0
 */
public interface KafkaSerializer extends Serializer<Tuple>, Deserializer<Tuple> {

	default void configure(Map<String, ?> configs, boolean isKey) {
	}
	
	default void close() {
	}

}
