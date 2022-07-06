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
package io.atlantisframework.vortex.metric.api;

import java.util.HashMap;
import java.util.Map;

/**
 * 
 * BigIntMetric
 * 
 * @author Fred Feng
 *
 * @since 2.0.1
 */
public class BigIntMetric extends AbstractUserMetric<BigInt> {

	public BigIntMetric(long value, long timestamp) {
		this(new BigInt(value), timestamp);
	}

	public BigIntMetric(BigInt bigInt, long timestamp) {
		super(bigInt, timestamp, false);
	}

	@Override
	public UserMetric<BigInt> reset(UserMetric<BigInt> newMetric) {
		BigInt current = get();
		BigInt update = newMetric.get();
		long highestValue = Long.max(current.getHighestValue(), update.getHighestValue());
		long lowestValue = Long.min(current.getLowestValue(), update.getLowestValue());
		long totalValue = current.getTotalValue() - update.getTotalValue();
		long count = current.getCount() - update.getCount();
		long timestamp = newMetric.getTimestamp();
		return new BigIntMetric(new BigInt(highestValue, lowestValue, totalValue, count), timestamp);
	}

	@Override
	public UserMetric<BigInt> merge(UserMetric<BigInt> newMetric) {
		BigInt current = get();
		BigInt update = newMetric.get();
		long highestValue = Long.max(current.getHighestValue(), update.getHighestValue());
		long lowestValue = Long.min(current.getLowestValue(), update.getLowestValue());
		long totalValue = current.getTotalValue() + update.getTotalValue();
		long count = current.getCount() + update.getCount();
		long timestamp = newMetric.getTimestamp();
		return new BigIntMetric(new BigInt(highestValue, lowestValue, totalValue, count), timestamp);
	}

	@Override
	public Map<String, Object> toEntries() {
		BigInt bigInt = get();
		Map<String, Object> data = new HashMap<String, Object>(5);
		data.put("highestValue", bigInt.getHighestValue());
		data.put("lowestValue", bigInt.getLowestValue());
		data.put("middleValue", bigInt.getMiddleValue());
		data.put("count", bigInt.getCount());
		data.put("timestamp", getTimestamp());
		return data;
	}

}
