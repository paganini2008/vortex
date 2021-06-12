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

import indi.atlantis.framework.vortex.common.Tuple;

/**
 * 
 * BoolTypeHandler
 * 
 * @author Fred Feng
 *
 * @version 1.0
 */
public class BoolTypeHandler implements UserTypeHandler<Bool> {

	@Override
	public String getDataTypeName() {
		return "bool";
	}

	@Override
	public UserMetric<Bool> convertAsMetric(String identifier, String metric, long timestamp, Tuple tuple) {
		boolean value = tuple.getField("value", Boolean.class);
		return new BoolMetric(value, timestamp);
	}

	@Override
	public UserMetric<Bool> convertAsMetric(String identifier, String metric, long timestamp, UserMetric<Bool> metricUnit) {
		long yes = metricUnit.get().getYes();
		long no = metricUnit.get().getNo();
		return new BoolMetric(new Bool(yes, no), timestamp);
	}

	@Override
	public Tuple convertAsTuple(String topic, String identifier, String metric, long timestamp, UserMetric<Bool> metricUnit) {
		long yes = metricUnit.get().getYes();
		long no = metricUnit.get().getNo();
		Tuple tuple = Tuple.newOne(topic);
		tuple.setField("name", identifier);
		tuple.setField("metric", metric);
		tuple.setField("yes", yes);
		tuple.setField("no", no);
		tuple.setField("timestamp", timestamp);
		return tuple;
	}

}
