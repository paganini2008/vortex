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
package indi.atlantis.framework.vortex.metric;

import java.util.HashMap;
import java.util.Map;

/**
 * 
 * BigIntMetricSequencer
 * 
 * @author Fred Feng
 *
 * @since 2.0.1
 */
public class BigIntMetricSequencer extends GenericUserMetricSequencer<String, BigInt> {

	public BigIntMetricSequencer(MetricEvictionHandler<String, UserMetric<BigInt>> evictionHandler) {
		this(1, SpanUnit.MINUTE, 60, evictionHandler);
	}

	public BigIntMetricSequencer(int span, SpanUnit spanUnit, int bufferSize,
			MetricEvictionHandler<String, UserMetric<BigInt>> evictionHandler) {
		super(span, spanUnit, bufferSize, evictionHandler);
	}

	@Override
	protected Map<String, Object> renderNull(long timeInMs) {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("highestValue", 0L);
		map.put("middleValue", 0L);
		map.put("lowestValue", 0L);
		map.put("count", 0);
		map.put("timestamp", timeInMs);
		return map;
	}

}