package org.springdessert.framework.transporter;

import java.util.List;

import org.springdessert.framework.transporter.common.Tuple;

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
