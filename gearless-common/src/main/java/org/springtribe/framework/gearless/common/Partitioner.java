package org.springtribe.framework.gearless.common;

import java.util.List;

/**
 * 
 * Partitioner
 *
 * @author Jimmy Hoff
 * @version 1.0
 */
public interface Partitioner {

	<T> T selectChannel(Object data, List<T> channels);

}
