package indi.atlantis.framework.vortex.common.serializer;

import indi.atlantis.framework.vortex.common.Tuple;

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
