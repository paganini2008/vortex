package indi.atlantis.framework.vortex.common;

import java.util.Map;

/**
 * 
 * TransportClient
 * 
 * @author Jimmy Hoff
 *
 * @since 1.0
 */
public interface TransportClient {

	default void write(CharSequence json) {
		write(Tuple.byString(json.toString()));
	}

	default void write(String topic, Map<String, ?> kwargs) {
		Tuple tuple = Tuple.newOne(topic);
		tuple.append(kwargs);
		write(tuple);
	}

	void write(Tuple tuple);

	boolean isActive();

	default void close() {
	}

}