/**
* Copyright 2017-2021 Fred Feng (paganini.fy@gmail.com)

* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package indi.atlantis.framework.vortex;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import indi.atlantis.framework.vortex.common.NioClient;
import indi.atlantis.framework.vortex.common.Partitioner;
import indi.atlantis.framework.vortex.common.Tuple;
import io.netty.util.internal.ThreadLocalRandom;

/**
 * 
 * BenchmarkController
 *
 * @author Fred Feng
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
