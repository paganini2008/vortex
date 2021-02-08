package indi.atlantis.framework.vortex.buffer;

import java.util.List;

import indi.atlantis.framework.vortex.common.Tuple;

/**
 * 
 * BufferZone
 * 
 * @author Jimmy Hoff
 *
 * @since 1.0
 */
public interface BufferZone {

	static String DEFAULT_COLLECTION_NAME_PREFIX = "atlantis:framework:vortex:bufferzone:";

	default void setCollectionNamePrefix(String namePrefix, String subNamePrefix) {
	}

	void set(String collectionName, Tuple tuple) throws Exception;

	List<Tuple> get(String collectionName, int pullSize) throws Exception;

	long size(String collectionName) throws Exception;

}
