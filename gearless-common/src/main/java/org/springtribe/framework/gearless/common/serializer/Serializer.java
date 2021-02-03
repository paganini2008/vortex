package org.springtribe.framework.gearless.common.serializer;

import org.springtribe.framework.gearless.common.Tuple;

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
