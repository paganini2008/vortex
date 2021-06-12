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

import indi.atlantis.framework.vortex.Handler;
import indi.atlantis.framework.vortex.common.Tuple;

/**
 * 
 * GenericUserMetricHandler
 * 
 * @author Fred Feng
 *
 * @version 1.0
 */
public class GenericUserMetricHandler<V> implements Handler {

	private final String topic;
	private final UserMetricListener<V> listener;

	public GenericUserMetricHandler(String topic, UserMetricListener<V> listener) {
		this.topic = topic;
		this.listener = listener;
	}

	@Override
	public void onData(Tuple tuple) {
		String name = tuple.getField("name", String.class);
		String metric = tuple.getField("metric", String.class);
		long timestamp = tuple.getTimestamp();
		listener.onMerge(name, metric, timestamp, tuple);
	}

	@Override
	public String getTopic() {
		return topic;
	}

}
