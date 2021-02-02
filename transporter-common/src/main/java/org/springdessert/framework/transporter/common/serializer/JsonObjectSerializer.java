package org.springdessert.framework.transporter.common.serializer;

import java.io.IOException;
import java.nio.charset.Charset;

import org.springdessert.framework.transporter.common.Tuple;
import org.springdessert.framework.transporter.common.TupleImpl;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.github.paganini2008.devtools.CharsetUtils;
import com.github.paganini2008.devtools.io.SerializationException;

/**
 * 
 * JsonObjectSerializer
 *
 * @author Jimmy Hoff
 * @version 1.0
 */
public class JsonObjectSerializer implements Serializer {

	private static final String PING = "PING";
	private static final String PONG = "PONG";
	private final ObjectMapper objectMapper = new ObjectMapper();
	private final Charset charset;

	{
		objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
		objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
	}

	public JsonObjectSerializer() {
		this(CharsetUtils.UTF_8);
	}

	public JsonObjectSerializer(Charset charset) {
		this.charset = charset;
	}

	@Override
	public byte[] serialize(Tuple tuple) {
		try {
			String content = (String) tuple.getField(Tuple.KEYWORD_CONTENT);
			return content.getBytes(charset);
		} catch (Exception e) {
			throw new SerializationException(e);
		}
	}

	@Override
	public Tuple deserialize(byte[] bytes) {
		String content = new String(bytes, charset);
		if (PING.equals(content) || PONG.equals(content)) {
			return Tuple.byString(content);
		}
		try {
			return objectMapper.readValue(content, TupleImpl.class);
		} catch (IOException e) {
			throw new SerializationException(e);
		}
	}

	public ObjectMapper getObjectMapper() {
		return objectMapper;
	}

}
