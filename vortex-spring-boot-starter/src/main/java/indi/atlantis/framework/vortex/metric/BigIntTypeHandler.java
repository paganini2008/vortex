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

import indi.atlantis.framework.vortex.common.Tuple;

/**
 * 
 * BigIntTypeHandler
 * 
 * @author Fred Feng
 *
 * @version 1.0
 */
public class BigIntTypeHandler implements UserTypeHandler<BigInt> {

	@Override
	public String getDataTypeName() {
		return "bigint";
	}

	@Override
	public UserMetric<BigInt> convertAsMetric(String identifier, String metric, long timestamp, Tuple tuple) {
		if (tuple.hasField("value")) {
			long value = tuple.getField("value", Long.class);
			return new BigIntMetric(value, timestamp);
		} else {
			long highestValue = tuple.getField("highestValue", Long.class);
			long lowestValue = tuple.getField("lowestValue", Long.class);
			long totalValue = tuple.getField("totalValue", Long.class);
			long count = tuple.getField("count", Long.class);
			return new BigIntMetric(new BigInt(highestValue, lowestValue, totalValue, count), timestamp);
		}
	}

	@Override
	public UserMetric<BigInt> convertAsMetric(String identifier, String metric, long timestamp, UserMetric<BigInt> metricUnit) {
		BigInt bigInt = metricUnit.get();
		long highestValue = bigInt.getHighestValue();
		long lowestValue = bigInt.getLowestValue();
		long totalValue = bigInt.getTotalValue();
		long count = bigInt.getCount();
		return new BigIntMetric(new BigInt(highestValue, lowestValue, totalValue, count), timestamp);
	}

	@Override
	public Tuple convertAsTuple(String topic, String identifier, String metric, long timestamp, UserMetric<BigInt> metricUnit) {
		BigInt bigInt = metricUnit.get();
		long highestValue = bigInt.getHighestValue();
		long lowestValue = bigInt.getLowestValue();
		long totalValue = bigInt.getTotalValue();
		long count = bigInt.getCount();
		Tuple tuple = Tuple.newOne(topic);
		tuple.setField("name", identifier);
		tuple.setField("metric", metric);
		tuple.setField("highestValue", highestValue);
		tuple.setField("lowestValue", lowestValue);
		tuple.setField("totalValue", totalValue);
		tuple.setField("count", count);
		tuple.setField("timestamp", timestamp);
		return tuple;
	}

}
