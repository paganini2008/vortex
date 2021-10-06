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

import io.atlantisframework.vortex.common.Tuple;

/**
 * 
 * GenericUserMetricListener
 * 
 * @author Fred Feng
 *
 * @since 2.0.1
 */
public class GenericUserMetricListener<V> implements UserMetricListener<V> {

	private final MetricSequencer<String, UserMetric<V>> sequencer;
	private final UserTypeHandler<V> typeHandler;
	
	public GenericUserMetricListener(MetricSequencer<String, UserMetric<V>> sequencer, UserTypeHandler<V> typeHandler) {
		this.sequencer = sequencer;
		this.typeHandler = typeHandler;
	}

	public GenericUserMetricListener(UserMetricSequencer<String, V> sequencer, UserTypeHandler<V> typeHandler) {
		this.sequencer = sequencer;
		this.typeHandler = typeHandler;
	}

	@Override
	public void onMerge(String identifier, String metric, long timestamp, Tuple tuple) {
		UserMetric<V> userMetric = typeHandler.convertAsMetric(identifier, metric, timestamp, tuple);
		if (userMetric != null) {
			sequencer.update(identifier, metric, timestamp, userMetric, true);
		}
	}

	@Override
	public void onReset(String identifier, String metric, long timestamp, UserMetric<V> metricUnit) {
		UserMetric<V> userMetric = typeHandler.convertAsMetric(identifier, metric, timestamp, metricUnit);
		if (userMetric != null) {
			sequencer.update(identifier, metric, timestamp, new ResettableUserMetric<V>(userMetric), true);
		}
	}

	@Override
	public void onSync(String identifier, String metric, long timestamp, Tuple tuple, boolean merged) {
		UserMetric<V> userMetric = typeHandler.convertAsMetric(identifier, metric, timestamp, tuple);
		if (userMetric != null) {
			sequencer.update(identifier, metric, timestamp, userMetric, merged);
		}
	}

	@Override
	public MetricSequencer<String, UserMetric<V>> getMetricSequencer() {
		return sequencer;
	}

	@Override
	public UserTypeHandler<V> getTypeHandler() {
		return typeHandler;
	}

}
