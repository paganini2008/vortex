package indi.atlantis.framework.vortex.common;

import java.util.Map;

import com.github.paganini2008.devtools.beans.BeanUtils;

/**
 * 
 * Tuple
 * 
 * @author Fred Feng
 * @version 1.0
 */
public interface Tuple {

	static final String DEFAULT_TOPIC = "default";
	static final String KW_CONTENT = "content";
	static final String KW_TOPIC = "topic";
	static final String KW_LENGTH = "length";
	static final String PARTITIONER_NAME = "indi.atlantis.framework.vortex.common.Partitioner";

	static final Tuple PING = Tuple.byString("PING");
	static final Tuple PONG = Tuple.byString("PONG");

	boolean hasField(String fieldName);

	void setField(String fieldName, Object value);

	Object getField(String fieldName);

	default void setLength(int length) {
		setField(KW_LENGTH, length);
	}

	default Object getField(String fieldName, Object defaultValue) {
		Object value;
		if ((value = getField(fieldName)) == null) {
			value = defaultValue;
		}
		return value;
	}

	<T> T getField(String fieldName, Class<T> requiredType);

	default <T> T getField(String fieldName, Class<T> requiredType, T defaultValue) {
		T value;
		if ((value = getField(fieldName, requiredType)) == null) {
			value = defaultValue;
		}
		return value;
	}

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
			return (String) getField(KW_TOPIC, DEFAULT_TOPIC);
		} catch (RuntimeException e) {
			throw new TransportClientException("Don't use topic as key to put into Tuple because it is a keyword.", e);
		}
	}

	default String getContent() {
		return getField(KW_CONTENT, String.class);
	}

	default long getTimestamp() {
		return getField("timestamp", Long.class);
	}

	default String getPartitionerName() {
		return getField(PARTITIONER_NAME, String.class);
	}

	default int getLength() {
		return getField(KW_LENGTH, Integer.class, 0);
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
		tuple.setField(KW_TOPIC, topic);
		return tuple;
	}

	public static Tuple byString(String content) {
		Tuple tuple = newOne();
		tuple.setField(KW_CONTENT, content);
		return tuple;
	}

	public static Tuple wrap(Map<String, ?> kwargs) {
		Tuple tuple = newOne();
		tuple.append(kwargs);
		return tuple;
	}

}
