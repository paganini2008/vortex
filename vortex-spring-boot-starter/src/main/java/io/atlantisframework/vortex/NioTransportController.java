/**
* Copyright 2017-2022 Fred Feng (paganini.fy@gmail.com)

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
package io.atlantisframework.vortex;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.github.paganini2008.devtools.collection.MapUtils;

import io.atlantisframework.vortex.common.NioClient;
import io.atlantisframework.vortex.common.Partitioner;
import io.atlantisframework.vortex.common.Tuple;

/**
 * 
 * NioTransportController
 * 
 * @author Fred Feng
 *
 * @since 2.0.1
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
		List<String> list = Arrays.stream(serverInfos).map(info -> info.toString()).collect(Collectors.toList());
		return ResponseEntity.ok(list.toArray(new String[0]));
	}

}
