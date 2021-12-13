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
package io.atlantisframework.vortex.metric.api;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * 
 * NumericMetric
 * 
 * @author Fred Feng
 *
 * @since 2.0.1
 */
public class NumericMetric extends AbstractUserMetric<Numeric> {

	public NumericMetric(BigDecimal value, long timestamp) {
		this(new Numeric(value), timestamp);
	}

	public NumericMetric(Numeric numeric, long timestamp) {
		super(numeric, timestamp, false);
	}

	@Override
	public UserMetric<Numeric> reset(UserMetric<Numeric> newMetric) {
		Numeric current = get();
		Numeric update = newMetric.get();
		BigDecimal highestValue = current.getHighestValue().max(update.getHighestValue());
		BigDecimal lowestValue = current.getLowestValue().min(update.getLowestValue());
		BigDecimal totalValue = current.getTotalValue().subtract(update.getTotalValue());
		long count = current.getCount() - update.getCount();
		long timestamp = newMetric.getTimestamp();
		return new NumericMetric(new Numeric(highestValue, lowestValue, totalValue, count), timestamp);
	}

	@Override
	public UserMetric<Numeric> merge(UserMetric<Numeric> newMetric) {
		Numeric current = get();
		Numeric update = newMetric.get();
		BigDecimal highestValue = current.getHighestValue().max(update.getHighestValue());
		BigDecimal lowestValue = current.getLowestValue().min(update.getLowestValue());
		BigDecimal totalValue = current.getTotalValue().add(update.getTotalValue());
		long count = current.getCount() + update.getCount();
		long timestamp = newMetric.getTimestamp();
		return new NumericMetric(new Numeric(highestValue, lowestValue, totalValue, count), timestamp);
	}

	@Override
	public Map<String, Object> toEntries() {
		Numeric numeric = get();
		Map<String, Object> data = new HashMap<String, Object>(5);
		data.put("highestValue", numeric.getHighestValue());
		data.put("lowestValue", numeric.getLowestValue());
		data.put("middleValue", numeric.getMiddleValue());
		data.put("count", numeric.getCount());
		data.put("timestamp", getTimestamp());
		return data;
	}

}
