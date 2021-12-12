package io.atlantisframework.vortex.streaming;

import org.springframework.beans.factory.annotation.Autowired;

import io.atlantisframework.vortex.Handler;
import io.atlantisframework.vortex.common.MultiSelectionPartitioner;
import io.atlantisframework.vortex.common.NioClient;
import io.atlantisframework.vortex.common.Partitioner;
import io.atlantisframework.vortex.common.Tuple;

/**
 * 
 * TimeWindowHandler
 *
 * @author Fred Feng
 * @since 2.0.4
 */
public class TimeWindowHandler implements Handler {

	private static final String KEYWORD = "name";

	@Autowired
	private NioClient nioClient;

	@Autowired
	private Partitioner partitioner;

	@Override
	public void onData(Tuple tuple) {
		if (partitioner instanceof MultiSelectionPartitioner) {
			String dataStreamName = tuple.getField(KEYWORD, String.class);
			tuple.setField("topic", dataStreamName);
			nioClient.send(tuple, partitioner);
		}
	}

	@Override
	public String getTopic() {
		return "timeWindow";
	}

}
