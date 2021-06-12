/**
* Copyright 2021 Fred Feng (paganini.fy@gmail.com)

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

import indi.atlantis.framework.vortex.common.Tuple;

/**
 * 
 * NumericTypeHandler
 * 
 * @author Fred Feng
 *
 * @version 1.0
 */
public class NumericTypeHandler implements UserTypeHandler<Numeric> {

	@Override
	public String getDataTypeName() {
		return "numeric";
	}

	@Override
	public UserMetric<Numeric> convertAsMetric(String identifier, String metric, long timestamp, Tuple tuple) {
		if (tuple.hasField("value")) {
			BigDecimal value = tuple.getField("value", BigDecimal.class);
			return new NumericMetric(value, timestamp);
		} else {
			BigDecimal highestValue = tuple.getField("highestValue", BigDecimal.class);
			BigDecimal lowestValue = tuple.getField("lowestValue", BigDecimal.class);
			BigDecimal totalValue = tuple.getField("totalValue", BigDecimal.class);
			long count = tuple.getField("count", Long.class);
			return new NumericMetric(new Numeric(highestValue, lowestValue, totalValue, count), timestamp);
		}
	}

	@Override
	public UserMetric<Numeric> convertAsMetric(String identifier, String metric, long timestamp, UserMetric<Numeric> metricUnit) {
		Numeric numeric = metricUnit.get();
		BigDecimal highestValue = numeric.getHighestValue();
		BigDecimal lowestValue = numeric.getLowestValue();
		BigDecimal totalValue = numeric.getTotalValue();
		long count = numeric.getCount();
		return new NumericMetric(new Numeric(highestValue, lowestValue, totalValue, count), timestamp);
	}

	@Override
	public Tuple convertAsTuple(String topic, String identifier, String metric, long timestamp, UserMetric<Numeric> metricUnit) {
		Numeric numeric = metricUnit.get();
		BigDecimal highestValue = numeric.getHighestValue();
		BigDecimal lowestValue = numeric.getLowestValue();
		BigDecimal totalValue = numeric.getTotalValue();
		long count = numeric.getCount();

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
