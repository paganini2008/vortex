package indi.atlantis.framework.vortex;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.github.paganini2008.devtools.ArrayUtils;
import com.github.paganini2008.devtools.collection.MapUtils;

import indi.atlantis.framework.vortex.common.NioClient;
import indi.atlantis.framework.vortex.common.Partitioner;
import indi.atlantis.framework.vortex.common.Tuple;

/**
 * 
 * NioTransportController
 * 
 * @author Jimmy Hoff
 *
 * @since 1.0
 */
@RequestMapping("/application/cluster/transport")
@RestController
public class NioTransportController {

	@Autowired
	private NioClient nioClient;

	@Autowired
	private Partitioner partitioner;

	@Autowired
	private NioTransportContext context;

	@Autowired
	private Accumulator accumulator;

	@GetMapping("/health")
	public ResponseEntity<Map<String, Object>> health() {
		Map<String, Object> data = new HashMap<String, Object>();
		data.put("fragments", accumulator.fragments());
		data.put("summaries", accumulator.summaries());
		return ResponseEntity.ok(data);
	}

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

	@GetMapping("/tcp/services")
	public ResponseEntity<String[]> tcpServices() {
		ServerInfo[] serverInfos = context.getServerInfos();
		String[] services = ArrayUtils.map(serverInfos, String.class, info -> {
			return info.toString();
		});
		return ResponseEntity.ok(services);
	}

}
