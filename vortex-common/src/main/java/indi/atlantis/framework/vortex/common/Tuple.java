package indi.atlantis.framework.vortex.common;

import java.util.Map;

import com.github.paganini2008.devtools.beans.BeanUtils;

/**
 * 
 * Tuple
 * 
 * @author Jimmy Hoff
 * @version 1.0
 */
public interface Tuple {

	static final String DEFAULT_TOPIC = "default";
	static final String KEYWORD_CONTENT = "content";
	static final String KEYWORD_TOPIC = "topic";

	static final String PARTITIONER_NAME = "org.springtribe.framework.gearless.common.Partitioner";

	static final Tuple PING = Tuple.byString("PING");
	static final Tuple PONG = Tuple.byString("PONG");

	boolean hasField(String fieldName);

	void setField(String fieldName, Object value);

	Object getField(String fieldName);

	default Object getField(String fieldName, Object defaultValue) {
		Object value;
		if ((value = getField(fieldName)) == null) {
			value = defaultValue;
		}
		return value;
	}

	<T> T getField(String fieldName, Class<T> requiredType);

	void append(Map<String, ?> m);

	void fill(Object object);

	default <T> T toBean(Class<T> requiredType) {
		final T object = BeanUtils.instantiate(requiredType);
		fill(object);
		return object;
	}

	Map<String, Object> toMap();

	Tuple copy();

	default String getTopic() {
		try {
			return (String) getField(KEYWORD_TOPIC, DEFAULT_TOPIC);
		} catch (RuntimeException e) {
			throw new TransportClientException("Don't use topic as key to put into Tuple because it is a keyword.", e);
		}
	}

	default String getContent() {
		return getField(KEYWORD_CONTENT, String.class);
	}

	default long getTimestamp() {
		return getField("timestamp", Long.class);
	}

	default String getPartitionerName() {
		return getField(PARTITIONER_NAME, String.class);
	}

	default boolean isPing() {
		return "PING".equalsIgnoreCase(getContent());
	}

	default boolean isPong() {
		return "PONG".equalsIgnoreCase(getContent());
	}

	public static Tuple newOne() {
		return newOne(DEFAULT_TOPIC);
	}

	public static Tuple newOne(String topic) {
		Tuple tuple = new TupleImpl();
		tuple.setField(KEYWORD_TOPIC, topic);
		return tuple;
	}

	public static Tuple byString(String content) {
		Tuple tuple = newOne();
		tuple.setField(KEYWORD_CONTENT, content);
		return tuple;
	}

	public static Tuple wrap(Map<String, ?> kwargs) {
		Tuple tuple = newOne();
		tuple.append(kwargs);
		return tuple;
	}

}
