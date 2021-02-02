package org.springdessert.framework.transporter;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.github.paganini2008.devtools.ArrayUtils;
import com.github.paganini2008.devtools.collection.MapUtils;
import org.springdessert.framework.transporter.common.NioClient;
import org.springdessert.framework.transporter.common.Partitioner;
import org.springdessert.framework.transporter.common.Tuple;

/**
 * 
 * ApplicationTransportController
 * 
 * @author Jimmy Hoff
 *
 * @since 1.0
 */
@RequestMapping("/application/cluster/transport")
@RestController
public class ApplicationTransportController {

	@Autowired
	private NioClient nioClient;

	@Autowired
	private Partitioner partitioner;

	@Autowired
	private ApplicationTransportContext context;

	@Qualifier("producer")
	@Autowired
	private Counter producer;

	@Qualifier("consumer")
	@Autowired
	private Counter consumer;

	@GetMapping("/emit")
	public ResponseEntity<Map<String, String>> emit0(HttpServletRequest request) {
		Map<String, String> parameterMap = MapUtils.toSingleValueMap(request.getParameterMap());
		Tuple data = Tuple.wrap(parameterMap);
		nioClient.send(data, partitioner);
		return ResponseEntity.ok(parameterMap);
	}

	@PostMapping("/emit")
	public ResponseEntity<Map<String, Object>> emit(@RequestBody Map<String, Object> body) {
		Tuple data = Tuple.wrap(body);
		nioClient.send(data, partitioner);
		return ResponseEntity.ok(body);
	}

	@GetMapping("/tps")
	public ResponseEntity<Map<String, Object>> tps() {
		Map<String, Object> data = new HashMap<String, Object>();
		data.put("producer", producer.getTps() + "/" + producer.getTotalTps());
		data.put("consumer", consumer.getTps() + "/" + consumer.getTotalTps());
		return ResponseEntity.ok(data);
	}

	@GetMapping("/tcp/services")
	public ResponseEntity<String[]> tcpServices() {
		ServerInfo[] serverInfos = context.getServerInfos();
		String[] services = ArrayUtils.map(serverInfos, info -> {
			return info.toString();
		});
		return ResponseEntity.ok(services);
	}

}
