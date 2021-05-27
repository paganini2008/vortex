package indi.atlantis.framework.vortex;

import java.util.List;

import indi.atlantis.framework.vortex.common.Tuple;

/**
 * 
 * BulkHandler
 * 
 * @author Fred Feng
 *
 * @since 1.0
 */
public interface BulkHandler {

	void onBatch(String topic, List<Tuple> list);

}
