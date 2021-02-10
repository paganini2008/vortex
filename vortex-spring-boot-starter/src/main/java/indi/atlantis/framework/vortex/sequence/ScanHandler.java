package indi.atlantis.framework.vortex.sequence;

import java.util.Map;

/**
 * 
 * ScanHandler
 *
 * @author Jimmy Hoff
 * @version 1.0
 */
public interface ScanHandler<I, T extends Metric<T>> {

	void handleSequence(I identifier, String metric, Map<String, T> data);

}
