/**
* Copyright 2018-2021 Fred Feng (paganini.fy@gmail.com)

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
package indi.atlantis.framework.vortex.common.serializer;

import com.github.paganini2008.devtools.io.SerializationUtils;

import indi.atlantis.framework.vortex.common.Tuple;

/**
 * 
 * JdkSerializer
 * 
 * @author Fred Feng
 *
 * @version 1.0
 */
public class JdkSerializer implements Serializer {

	private final boolean compress;

	public JdkSerializer() {
		this(false);
	}

	public JdkSerializer(boolean compress) {
		this.compress = compress;
	}

	public byte[] serialize(Tuple tuple) {
		return SerializationUtils.serialize(tuple, compress);
	}

	public Tuple deserialize(byte[] bytes) {
		Tuple tuple = (Tuple) SerializationUtils.deserialize(bytes, compress);
		tuple.setLength(bytes.length);
		return tuple;
	}

}
