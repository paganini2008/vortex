package indi.atlantis.framework.vortex.metric;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import indi.atlantis.framework.tridenter.utils.ApplicationContextUtils;
import indi.atlantis.framework.vortex.common.NioClient;
import indi.atlantis.framework.vortex.common.Partitioner;
import indi.atlantis.framework.vortex.common.Tuple;

/**
 * 
 * MetricSequencerController
 *
 * @author Jimmy Hoff
 * @version 1.0
 */
@RestController
@RequestMapping("/metrics")
public class MetricSequencerController {

	@Autowired
	private NioClient nioClient;

	@Autowired
	private Partitioner partitioner;

	@Qualifier("bigintMetricRegistrar")
	@Autowired
	private MetricRegistrar bigintMetricRegistrar;

	@Qualifier("numericMetricRegistrar")
	@Autowired
	private MetricRegistrar numericMetricRegistrar;

	@Qualifier("boolMetricRegistrar")
	@Autowired
	private MetricRegistrar boolMetricRegistrar;

	@GetMapping("/sequence/{dataType}/{name}/{metric}")
	public Result sequence(@PathVariable("dataType") String dataType, @PathVariable("name") String name,
			@PathVariable("metric") String metric, @RequestParam(name = "asc", required = false, defaultValue = "true") boolean asc) {
		Result result = new Result(dataType, name, metric);
		Map<String, Map<String, Object>> data = new HashMap<String, Map<String, Object>>();
		switch (dataType.toLowerCase()) {
		case "bool":
			data = boolMetricRegistrar.getSequencer().sequence(name, metric, asc, bigintMetricRegistrar.getRender());
			break;
		case "long":
			data = bigintMetricRegistrar.getSequencer().sequence(name, metric, asc, bigintMetricRegistrar.getRender());
			break;
		case "decimal":
			data = numericMetricRegistrar.getSequencer().sequence(name, metric, asc, numericMetricRegistrar.getRender());
			break;
		default:
			MetricRegistrar target = ApplicationContextUtils.getBean(dataType + "MetricRegistrar", MetricRegistrar.class);
			if (target != null) {
				data = target.getSequencer().sequence(name, metric, asc, target.getRender());
			}
			break;
		}
		result.setData(data);
		return result;
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

}
