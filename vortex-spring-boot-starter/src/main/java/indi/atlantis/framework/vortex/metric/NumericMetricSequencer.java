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

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * 
 * NumericMetricSequencer
 * 
 * @author Fred Feng
 *
 * @version 1.0
 */
public class NumericMetricSequencer extends GenericUserMetricSequencer<String, Numeric> {

	public NumericMetricSequencer(MetricEvictionHandler<String, UserMetric<Numeric>> evictionHandler) {
		this(1, SpanUnit.MINUTE, 60, evictionHandler);
	}

	public NumericMetricSequencer(int span, SpanUnit spanUnit, int bufferSize,
			MetricEvictionHandler<String, UserMetric<Numeric>> evictionHandler) {
		super(span, spanUnit, bufferSize, evictionHandler);
	}

	@Override
	protected Map<String, Object> renderNull(long timeInMs) {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("highestValue", BigDecimal.ZERO);
		map.put("middleValue", BigDecimal.ZERO);
		map.put("lowestValue", BigDecimal.ZERO);
		map.put("count", 0L);
		map.put("timestamp", timeInMs);
		return map;
	}

}
