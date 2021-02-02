package org.springdessert.framework.transporter.common.serializer;

import org.springdessert.framework.transporter.common.Tuple;

/**
 * 
 * Serializer
 *
 * @author Jimmy Hoff
 * @version 1.0
 */
public interface Serializer {
	
	byte[] serialize(Tuple tuple);

	Tuple deserialize(byte[] bytes);

}
