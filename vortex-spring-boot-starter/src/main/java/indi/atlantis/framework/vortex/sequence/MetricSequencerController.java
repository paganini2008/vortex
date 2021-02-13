package indi.atlantis.framework.vortex.sequence;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.LinkedHashMap;
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
@RequestMapping("/metric")
public class MetricSequencerController {

	@Autowired
	private NioClient nioClient;

	@Autowired
	private Partitioner partitioner;

	@Qualifier("secondaryEnvironment")
	@Autowired
	private Environment environment;

	@GetMapping("/echo")
	public Map<String, Object> echo(@RequestParam("q") String q) {
		return Collections.singletonMap("q", q);
	}

	@GetMapping("/sequence/{dataType}/{name}/{metric}")
	public Map<String, Map<String, Object>> sequence(@PathVariable("dataType") String dataType, @PathVariable("name") String name,
			@PathVariable("metric") String metric) {
		Map<String, Map<String, Object>> data = new LinkedHashMap<String, Map<String, Object>>();
		switch (dataType) {
		case "bool":
			MetricSequencer<String, UserMetric<Bool>> boolMetricSequencer = environment.boolMetricSequencer();
			Map<String, UserMetric<Bool>> boolSequence = boolMetricSequencer.sequence(name, metric);
			for (Map.Entry<String, UserMetric<Bool>> entry : boolSequence.entrySet()) {
				data.put(entry.getKey(), entry.getValue().get().toEntries());
			}
			return DataRenderer.renderBoolMetric(data, boolMetricSequencer.getSpanUnit(), boolMetricSequencer.getSpan(),
					boolMetricSequencer.getBufferSize());
		case "long":
			MetricSequencer<String, NumberMetric<Long>> longMetricSequencer = environment.longMetricSequencer();
			Map<String, NumberMetric<Long>> longSequence = longMetricSequencer.sequence(name, metric);
			for (Map.Entry<String, NumberMetric<Long>> entry : longSequence.entrySet()) {
				data.put(entry.getKey(), entry.getValue().toEntries());
			}
			return DataRenderer.renderNumberMetric(data, longMetricSequencer.getSpanUnit(), longMetricSequencer.getSpan(),
					longMetricSequencer.getBufferSize());
		case "double":
			MetricSequencer<String, NumberMetric<Double>> doubleMetricSequencer = environment.doubleMetricSequencer();
			Map<String, NumberMetric<Double>> doubleSequence = doubleMetricSequencer.sequence(name, metric);
			for (Map.Entry<String, NumberMetric<Double>> entry : doubleSequence.entrySet()) {
				data.put(entry.getKey(), entry.getValue().toEntries());
			}
			return DataRenderer.renderNumberMetric(data, doubleMetricSequencer.getSpanUnit(), doubleMetricSequencer.getSpan(),
					doubleMetricSequencer.getBufferSize());
		case "decimal":
			MetricSequencer<String, NumberMetric<BigDecimal>> decimalMetricSequencer = environment.decimalMetricSequencer();
			Map<String, NumberMetric<BigDecimal>> decimalSequence = decimalMetricSequencer.sequence(name, metric);
			for (Map.Entry<String, NumberMetric<BigDecimal>> entry : decimalSequence.entrySet()) {
				data.put(entry.getKey(), entry.getValue().toEntries());
			}
			return DataRenderer.renderNumberMetric(data, decimalMetricSequencer.getSpanUnit(), decimalMetricSequencer.getSpan(),
					decimalMetricSequencer.getBufferSize());
		default:
			throw new UnsupportedOperationException(dataType);
		}
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
