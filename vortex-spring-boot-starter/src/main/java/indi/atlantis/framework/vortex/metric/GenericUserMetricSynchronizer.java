package indi.atlantis.framework.vortex.metric;

import java.net.SocketAddress;
import java.util.Map;

import indi.atlantis.framework.vortex.common.NioClient;
import indi.atlantis.framework.vortex.common.Tuple;

/**
 * 
 * GenericUserMetricSynchronizer
 * 
 * @author Jimmy Hoff
 *
 * @version 1.0
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
			for (Map.Entry<String, UserMetric<V>> entry : data.entrySet()) {
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
