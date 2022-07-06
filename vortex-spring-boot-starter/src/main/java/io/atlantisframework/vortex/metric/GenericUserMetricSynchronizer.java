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
package io.atlantisframework.vortex.metric;

import java.net.SocketAddress;
import java.time.Instant;
import java.util.Map;

import io.atlantisframework.vortex.common.NioClient;
import io.atlantisframework.vortex.common.Tuple;
import io.atlantisframework.vortex.metric.api.UserMetric;

/**
 * 
 * GenericUserMetricSynchronizer
 * 
 * @author Fred Feng
 *
 * @since 2.0.1
 */
public class GenericUserMetricSynchronizer<V> implements Synchronizer {

	private final String topic;
	private final UserMetricListener<V> listener;
	private final boolean incremental;

	public GenericUserMetricSynchronizer(String topic, boolean incremental, UserMetricListener<V> listener) {
		this.topic = topic;
		this.incremental = incremental;
		this.listener = listener;
	}

	public void synchronize(NioClient nioClient, SocketAddress remoteAddress) {
		listener.getMetricSequencer().scan((name, metric, data) -> {
			UserMetric<V> metricUnit;
			long timestamp;
			for (Map.Entry<Instant, UserMetric<V>> entry : data.entrySet()) {
				metricUnit = entry.getValue();
				timestamp = metricUnit.getTimestamp();
				Tuple tuple = listener.getTypeHandler().convertAsTuple(topic, name, metric, timestamp, metricUnit);
				if (tuple != null) {
					nioClient.send(remoteAddress, tuple);
				}
				if (incremental) {
					listener.onReset(name, metric, timestamp, metricUnit);
				}
			}
		});
	}

}
