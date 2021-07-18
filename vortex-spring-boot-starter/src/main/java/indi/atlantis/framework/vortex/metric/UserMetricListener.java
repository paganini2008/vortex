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
package indi.atlantis.framework.vortex.metric;

import indi.atlantis.framework.vortex.common.Tuple;

/**
 * 
 * UserMetricListener
 * 
 * @author Fred Feng
 *
 * @version 1.0
 */
public interface UserMetricListener<V> {

	void onMerge(String identifier, String metric, long timestamp, Tuple tuple);

	void onReset(String identifier, String metric, long timestamp, UserMetric<V> metricUnit);

	void onSync(String identifier, String metric, long timestamp, Tuple tuple, boolean merged);

	UserTypeHandler<V> getTypeHandler();

	MetricSequencer<String, UserMetric<V>> getMetricSequencer();

}