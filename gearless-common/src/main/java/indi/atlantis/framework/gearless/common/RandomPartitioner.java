package indi.atlantis.framework.gearless.common;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 
 * RandomPartitioner
 * 
 * @author Jimmy Hoff
 * 
 * 
 * @version 1.0
 */
public class RandomPartitioner implements Partitioner {

	public <T> T selectChannel(Object data, List<T> channels) {
		try {
			return channels.get(ThreadLocalRandom.current().nextInt(channels.size()));
		} catch (RuntimeException e) {
			return null;
		}
	}

}
