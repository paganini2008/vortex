/**
* Copyright 2018-2021 Fred Feng (paganini.fy@gmail.com)

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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health.Builder;
import org.springframework.boot.actuate.health.Status;

/**
 * 
 * NioTransportHealthIndicator
 *
 * @author Fred Feng
 * @version 1.0
 */
public class NioTransportHealthIndicator extends AbstractHealthIndicator {

	@Autowired
	private Accumulator accumulator;

	@Override
	protected void doHealthCheck(Builder builder) throws Exception {
		long tps = accumulator.getTps();
		if (tps < 2000) {
			builder.status(new Status("Low Payload"));
		} else if (tps < 5000) {
			builder.status(new Status("Middle Payload"));
		} else if (tps < 10000) {
			builder.status(new Status("Higher Payload"));
		} else {
			builder.status(new Status("High Payload"));
		}
		Map<String, Object> detail = new LinkedHashMap<String, Object>();
		detail.put("count", accumulator.getCount());
		detail.put("length", accumulator.getLength());
		detail.put("tps", accumulator.getTps());
		detail.put("state", accumulator.isIdleTimeout(1, TimeUnit.MINUTES) ? "idle" : "active");
		builder.withDetails(detail);
	}

}
