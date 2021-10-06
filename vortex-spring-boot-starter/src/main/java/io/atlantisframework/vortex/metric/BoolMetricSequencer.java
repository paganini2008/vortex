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
package io.atlantisframework.vortex.metric;

import java.util.HashMap;
import java.util.Map;

/**
 * 
 * BoolMetricSequencer
 *
 * @author Fred Feng
 * @since 2.0.1
 */
public class BoolMetricSequencer extends GenericUserMetricSequencer<String, Bool> {

	public BoolMetricSequencer(MetricEvictionHandler<String, UserMetric<Bool>> evictionHandler) {
		this(1, SpanUnit.MINUTE, 60, evictionHandler);
	}

	public BoolMetricSequencer(int span, SpanUnit spanUnit, int bufferSize,
			MetricEvictionHandler<String, UserMetric<Bool>> evictionHandler) {
		super(span, spanUnit, bufferSize, evictionHandler);
	}

	@Override
	protected Map<String, Object> renderNull(long timeInMs) {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("yes", 0L);
		map.put("no", 0L);
		map.put("timestamp", timeInMs);
		return map;
	}

}
