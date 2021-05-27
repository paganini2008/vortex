package indi.atlantis.framework.vortex.metric;

import java.util.Map;

/**
 * 
 * ScanHandler
 *
 * @author Fred Feng
 * @version 1.0
 */
public interface ScanHandler<I, T extends Metric<T>> {

	void handleSequence(I identifier, String metric, Map<String, T> data);

}
