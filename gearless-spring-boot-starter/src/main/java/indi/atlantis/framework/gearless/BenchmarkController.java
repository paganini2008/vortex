package indi.atlantis.framework.gearless;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import indi.atlantis.framework.gearless.common.NioClient;
import indi.atlantis.framework.gearless.common.Partitioner;
import indi.atlantis.framework.gearless.common.Tuple;
import io.netty.util.internal.ThreadLocalRandom;

/**
 * 
 * BenchmarkController
 *
 * @author Jimmy Hoff
 * 
 * @since 1.0
 */
@RequestMapping("/application/cluster/transport")
@RestController
public class BenchmarkController {

	@Autowired
	private NioClient nioClient;

	@Autowired
	private Partitioner partitioner;

	@GetMapping("/echo")
	public Map<String, Object> echo(@RequestParam("q") String content) {
		Tuple data = Tuple.byString(UUID.randomUUID().toString());
		nioClient.send(data, partitioner);
		Map<String, Object> result = new HashMap<String, Object>();
		result.put("q", content);
		result.put("success", true);
		return result;
	}

	@GetMapping("/benchmark")
	public Map<String, Object> benchmark(@RequestParam(name = "n", defaultValue = "10000", required = false) int N) {
		for (int i = 0; i < N; i++) {
			StringBuilder str = new StringBuilder();
			for (int j = 0, l = ThreadLocalRandom.current().nextInt(10, 100); j < l; j++) {
				str.append(UUID.randomUUID().toString());
			}
			Tuple data = Tuple.byString(str.toString());
			nioClient.send(data, partitioner);
		}

		Map<String, Object> result = new HashMap<String, Object>();
		result.put("success", true);
		return result;
	}

}
