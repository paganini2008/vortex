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
package indi.atlantis.framework.vortex.common.embeddedio;

import com.github.paganini2008.embeddedio.Serialization;

import indi.atlantis.framework.vortex.common.Tuple;
import indi.atlantis.framework.vortex.common.serializer.KryoSerializer;
import indi.atlantis.framework.vortex.common.serializer.Serializer;

/**
 * 
 * EmbeddedSerializationFactory
 *
 * @author Fred Feng
 * @since 1.0
 */
public class EmbeddedSerializationFactory implements SerializationFactory {

	private final Serializer serializer;

	public EmbeddedSerializationFactory() {
		this(new KryoSerializer());
	}

	public EmbeddedSerializationFactory(Serializer serializer) {
		this.serializer = serializer;
	}

	@Override
	public Serialization getEncoder() {
		return new Serialization() {

			@Override
			public byte[] serialize(Object object) {
				return serializer.serialize((Tuple) object);
			}

			@Override
			public Tuple deserialize(byte[] bytes) {
				return (Tuple) serializer.deserialize(bytes);
			}
		};
	}

	@Override
	public Serialization getDecoder() {
		return getEncoder();
	}

}
