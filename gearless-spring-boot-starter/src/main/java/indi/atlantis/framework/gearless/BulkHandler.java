package indi.atlantis.framework.gearless;

import java.util.List;

import indi.atlantis.framework.gearless.common.Tuple;

/**
 * 
 * BulkHandler
 * 
 * @author Jimmy Hoff
 *
 * @since 1.0
 */
public interface BulkHandler {

	void onBatch(List<Tuple> list);

}
