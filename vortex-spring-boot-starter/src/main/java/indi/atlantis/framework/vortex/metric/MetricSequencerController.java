package indi.atlantis.framework.vortex.metric;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Date;
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

import com.github.paganini2008.devtools.date.DateUtils;

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

	@Qualifier("secondaryEnvironment")
	@Autowired
	private Environment environment;

	@GetMapping("/sequence/{dataType}/{name}/{metric}")
	public Result sequence(@PathVariable("dataType") String dataType, @PathVariable("name") String name,
			@PathVariable("metric") String metric,
			@RequestParam(name = "rendered", required = false, defaultValue = "true") boolean rendered,
			@RequestParam(name = "asc", required = false, defaultValue = "true") boolean asc) {
		Result result = new Result(dataType, name, metric);
		Map<String, Map<String, Object>> data = new LinkedHashMap<String, Map<String, Object>>();
		long timestamp = System.currentTimeMillis();
		switch (dataType.toLowerCase()) {
		case "bool":
			MetricSequencer<String, UserMetric<Bool>> boolMetricSequencer = environment.boolMetricSequencer();
			Map<String, UserMetric<Bool>> boolSequence = boolMetricSequencer.sequence(name, metric);
			for (Map.Entry<String, UserMetric<Bool>> entry : boolSequence.entrySet()) {
				data.put(entry.getKey(), entry.getValue().toEntries());
				timestamp = timestamp > 0 ? Math.min(entry.getValue().getTimestamp(), timestamp) : entry.getValue().getTimestamp();
			}
			if (rendered) {
				Date startTime;
				if (asc) {
					Date date = new Date(timestamp);
					int amount = boolMetricSequencer.getSpan() * boolMetricSequencer.getBufferSize();
					Date endTime = DateUtils.addField(date, boolMetricSequencer.getSpanUnit().getCalendarField(), amount);
					if (endTime.compareTo(new Date()) <= 0) {
						asc = false;
						startTime = new Date();
					} else {
						startTime = date;
					}
				} else {
					startTime = new Date();
				}
				data = DataRenderer.renderBoolMetric(data, startTime, asc, boolMetricSequencer.getSpanUnit(), boolMetricSequencer.getSpan(),
						boolMetricSequencer.getBufferSize());
			}
			break;
		case "long":
			MetricSequencer<String, NumberMetric<Long>> longMetricSequencer = environment.longMetricSequencer();
			Map<String, NumberMetric<Long>> longSequence = longMetricSequencer.sequence(name, metric);
			for (Map.Entry<String, NumberMetric<Long>> entry : longSequence.entrySet()) {
				data.put(entry.getKey(), entry.getValue().toEntries());
				timestamp = timestamp > 0 ? Math.min(entry.getValue().getTimestamp(), timestamp) : entry.getValue().getTimestamp();
			}
			if (rendered) {
				Date startTime;
				if (asc) {
					Date date = new Date(timestamp);
					int amount = longMetricSequencer.getSpan() * longMetricSequencer.getBufferSize();
					Date endTime = DateUtils.addField(date, longMetricSequencer.getSpanUnit().getCalendarField(), amount);
					if (endTime.compareTo(new Date()) <= 0) {
						asc = false;
						startTime = new Date();
					} else {
						startTime = date;
					}
				} else {
					startTime = new Date();
				}
				data = DataRenderer.renderNumberMetric(data, startTime, asc, longMetricSequencer.getSpanUnit(),
						longMetricSequencer.getSpan(), longMetricSequencer.getBufferSize());
			}
			break;
		case "double":
			MetricSequencer<String, NumberMetric<Double>> doubleMetricSequencer = environment.doubleMetricSequencer();
			Map<String, NumberMetric<Double>> doubleSequence = doubleMetricSequencer.sequence(name, metric);
			for (Map.Entry<String, NumberMetric<Double>> entry : doubleSequence.entrySet()) {
				data.put(entry.getKey(), entry.getValue().toEntries());
				timestamp = timestamp > 0 ? Math.min(entry.getValue().getTimestamp(), timestamp) : entry.getValue().getTimestamp();
			}
			if (rendered) {
				Date startTime;
				if (asc) {
					Date date = new Date(timestamp);
					int amount = doubleMetricSequencer.getSpan() * doubleMetricSequencer.getBufferSize();
					Date endTime = DateUtils.addField(date, doubleMetricSequencer.getSpanUnit().getCalendarField(), amount);
					if (endTime.compareTo(new Date()) <= 0) {
						asc = false;
						startTime = new Date();
					} else {
						startTime = date;
					}
				} else {
					startTime = new Date();
				}
				data = DataRenderer.renderNumberMetric(data, startTime, asc, doubleMetricSequencer.getSpanUnit(),
						doubleMetricSequencer.getSpan(), doubleMetricSequencer.getBufferSize());
			}
			break;
		case "decimal":
			MetricSequencer<String, NumberMetric<BigDecimal>> decimalMetricSequencer = environment.decimalMetricSequencer();
			Map<String, NumberMetric<BigDecimal>> decimalSequence = decimalMetricSequencer.sequence(name, metric);
			for (Map.Entry<String, NumberMetric<BigDecimal>> entry : decimalSequence.entrySet()) {
				data.put(entry.getKey(), entry.getValue().toEntries());
				timestamp = timestamp > 0 ? Math.min(entry.getValue().getTimestamp(), timestamp) : entry.getValue().getTimestamp();
			}
			if (rendered) {
				Date startTime;
				if (asc) {
					Date date = new Date(timestamp);
					int amount = decimalMetricSequencer.getSpan() * decimalMetricSequencer.getBufferSize();
					Date endTime = DateUtils.addField(date, decimalMetricSequencer.getSpanUnit().getCalendarField(), amount);
					if (endTime.compareTo(new Date()) <= 0) {
						asc = false;
						startTime = new Date();
					} else {
						startTime = date;
					}
				} else {
					startTime = new Date();
				}
				data = DataRenderer.renderNumberMetric(data, startTime, asc, decimalMetricSequencer.getSpanUnit(),
						decimalMetricSequencer.getSpan(), decimalMetricSequencer.getBufferSize());
			}
			break;
		default:
			throw new UnsupportedOperationException(dataType);
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
