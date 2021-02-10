package indi.atlantis.framework.vortex.sequence;

import java.util.Collections;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import indi.atlantis.framework.vortex.common.NioClient;
import indi.atlantis.framework.vortex.common.Partitioner;
import indi.atlantis.framework.vortex.common.Tuple;

/**
 * 
 * MetricSequenceController
 *
 * @author Jimmy Hoff
 * @version 1.0
 */
@RestController
public class MetricSequenceController {

	@Autowired
	private NioClient nioClient;

	@Autowired
	private Partitioner partitioner;

	@PostMapping("/{type}/sequence")
	public Map<String, Object> sequenceLong(@PathVariable("type") String type, @RequestBody SequenceRequest sequenceRequest) {
		Tuple tuple = Tuple.newOne(type);
		tuple.setField("name", sequenceRequest.getName());
		tuple.setField("metric", sequenceRequest.getMetric());
		tuple.setField("value", sequenceRequest.getValue());
		tuple.setField("timestamp", sequenceRequest.getTimestamp());
		nioClient.send(tuple, partitioner);
		return Collections.singletonMap("ok", 1);
	}

}
