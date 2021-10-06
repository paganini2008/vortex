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

import io.atlantisframework.vortex.Handler;
import io.atlantisframework.vortex.common.Tuple;

/**
 * 
 * GenericUserMetricSynchronizationHandler
 * 
 * @author Fred Feng
 *
 * @since 2.0.1
 */
public class GenericUserMetricSynchronizationHandler<V> implements Handler {

	private final String topic;
	private final UserMetricListener<V> listener;
	private final boolean merged;

	public GenericUserMetricSynchronizationHandler(String topic, boolean merged, UserMetricListener<V> listener) {
		this.topic = topic;
		this.merged = merged;
		this.listener = listener;
	}

	@Override
	public void onData(Tuple tuple) {
		String name = tuple.getField("name", String.class);
		String metric = tuple.getField("metric", String.class);
		long timestamp = tuple.getTimestamp();
		listener.onSync(name, metric, timestamp, tuple, merged);
	}

	@Override
	public String getTopic() {
		return topic;
	}

}
