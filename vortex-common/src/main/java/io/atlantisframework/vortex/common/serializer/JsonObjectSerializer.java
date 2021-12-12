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
package io.atlantisframework.vortex.common.serializer;

import java.io.IOException;
import java.nio.charset.Charset;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.paganini2008.devtools.CharsetUtils;
import com.github.paganini2008.devtools.io.SerializationException;

import io.atlantisframework.vortex.common.Tuple;
import io.atlantisframework.vortex.common.TupleImpl;

/**
 * 
 * JsonObjectSerializer
 *
 * @author Fred Feng
 * @since 2.0.1
 */
public class JsonObjectSerializer implements Serializer {

	private static final String KEYWORD = "content";
	private static final String PING = "PING";
	private static final String PONG = "PONG";
	private final ObjectMapper objectMapper = new ObjectMapper();
	private final Charset charset;

	{
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
			String content = tuple.getField(KEYWORD, String.class);
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
		Tuple tuple;
		try {
			tuple = objectMapper.readValue(content, TupleImpl.class);
			tuple.setLength(bytes.length);
		} catch (IOException e) {
			throw new SerializationException(e);
		}
		return tuple;
	}

	public ObjectMapper getObjectMapper() {
		return objectMapper;
	}

}
