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
package indi.atlantis.framework.vortex.utils;

import java.util.Map;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.util.Pool;

import indi.atlantis.framework.vortex.common.Tuple;
import indi.atlantis.framework.vortex.common.TupleImpl;
import indi.atlantis.framework.vortex.common.serializer.KryoUtils;

/**
 * 
 * KryoKafkaSerializer
 * 
 * @author Fred Feng
 *
 * @since 1.0
 */
public class KryoKafkaSerializer implements KafkaSerializer {

	private static final int DEFAULT_POOL_SIZE = 16;
	private static final int DEFAULT_IO_POOL_SIZE = 128;
	private static final int DEFAULT_POOL_BUFFER_SIZE = 1024 * 1024;

	private Pool<Kryo> pool;
	private Pool<Output> outputPool;
	private Pool<Input> inputPool;

	@Override
	public void configure(Map<String, ?> configs, boolean isKey) {
		pool = KryoUtils.getPool(DEFAULT_POOL_SIZE);
		outputPool = KryoUtils.getOutputPool(DEFAULT_IO_POOL_SIZE, DEFAULT_POOL_BUFFER_SIZE);
		inputPool = KryoUtils.getInputPool(DEFAULT_IO_POOL_SIZE, DEFAULT_POOL_BUFFER_SIZE);
	}

	@Override
	public byte[] serialize(String topic, Tuple data) {
		Kryo kryo = pool.obtain();
		Output output = outputPool.obtain();
		try {
			output.reset();
			kryo.writeObject(output, data);
			return output.getBuffer();
		} finally {
			outputPool.free(output);
			pool.free(kryo);
		}
	}

	public Tuple deserialize(String topic, byte[] bytes) {
		Kryo kryo = pool.obtain();
		Input input = inputPool.obtain();
		try {
			input.setBuffer(bytes);
			return kryo.readObject(input, TupleImpl.class);
		} finally {
			inputPool.free(input);
			pool.free(kryo);
		}
	}

	@Override
	public void close() {
		pool.clear();
		outputPool.clear();
		inputPool.clear();
	}

}
