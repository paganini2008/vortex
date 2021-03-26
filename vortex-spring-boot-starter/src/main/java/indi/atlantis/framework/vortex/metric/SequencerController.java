package indi.atlantis.framework.vortex.metric;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.github.paganini2008.devtools.RandomUtils;

import indi.atlantis.framework.vortex.common.NioClient;
import indi.atlantis.framework.vortex.common.Partitioner;
import indi.atlantis.framework.vortex.common.Tuple;

/**
 * 
 * SequencerController
 *
 * @author Jimmy Hoff
 * @version 1.0
 */
@RestController
@RequestMapping("/metrics")
public class SequencerController {

	@Autowired
	private NioClient nioClient;

	@Autowired
	private Partitioner partitioner;

	@Autowired
	private Sequencer sequencer;

	@GetMapping("/sequence/{dataType}/{name}/{metric}")
	public Result sequence(@PathVariable("dataType") String dataType, @PathVariable("name") String name,
			@PathVariable("metric") String metric, @RequestParam(name = "asc", required = false, defaultValue = "true") boolean asc) {
		Result result = new Result(dataType, name, metric);
		Map<String, Map<String, Object>> data = sequencer.sequence(dataType, name, metric, asc);
		result.setData(data);
		return result;
	}

	@GetMapping("/sequence")
	public Map<String, Object> sequence(@RequestParam("dataType") String dataType, @RequestParam("name") String name,
			@RequestParam("metric") String metric, @RequestParam("value") BigDecimal value, @RequestParam("timestamp") long timestamp) {
		Tuple tuple = Tuple.newOne(dataType);
		tuple.setField("name", name);
		tuple.setField("metric", metric);
		tuple.setField("value", value);
		tuple.setField("timestamp", timestamp);
		nioClient.send(tuple, partitioner);
		return Collections.singletonMap("ok", 1);
	}

	@PostMapping("/sequence/{dataType}")
	public Map<String, Object> sequence(@PathVariable("dataType") String dataType, @RequestBody SequenceRequest sequenceRequest) {
		Tuple tuple = Tuple.newOne(dataType);
		tuple.setField("name", sequenceRequest.getName());
		tuple.setField("metric", sequenceRequest.getMetric());
		tuple.setField("value", sequenceRequest.getValue());
		tuple.setField("timestamp", sequenceRequest.getTimestamp());
		nioClient.send(tuple, partitioner);
		return Collections.singletonMap("ok", 1);
	}

	@GetMapping("/test/bigint/{name}/{metric}")
	public Map<String, Object> testBigInt(@PathVariable("name") String name, @PathVariable("metric") String metric) {
		Tuple tuple = Tuple.newOne("bigint");
		tuple.setField("name", name);
		tuple.setField("metric", metric);
		tuple.setField("value", RandomUtils.randomLong(100, 10000));
		tuple.setField("timestamp", System.currentTimeMillis());
		nioClient.send(tuple, partitioner);
		return Collections.singletonMap("ok", 1);
	}

	@GetMapping("/test/numeric/{name}/{metric}")
	public Map<String, Object> testNumeric(@PathVariable("name") String name, @PathVariable("metric") String metric) {
		Tuple tuple = Tuple.newOne("numeric");
		tuple.setField("name", name);
		tuple.setField("metric", metric);
		tuple.setField("value", BigDecimal.valueOf(RandomUtils.randomDouble(100, 10000)).setScale(4, RoundingMode.HALF_UP));
		tuple.setField("timestamp", System.currentTimeMillis());
		nioClient.send(tuple, partitioner);
		return Collections.singletonMap("ok", 1);
	}

	@GetMapping("/test/bool/{name}/{metric}")
	public Map<String, Object> testBool(@PathVariable("name") String name, @PathVariable("metric") String metric) {
		Tuple tuple = Tuple.newOne("bool");
		tuple.setField("name", name);
		tuple.setField("metric", metric);
		tuple.setField("value", RandomUtils.randomBoolean());
		tuple.setField("timestamp", System.currentTimeMillis());
		nioClient.send(tuple, partitioner);
		return Collections.singletonMap("ok", 1);
	}

}
