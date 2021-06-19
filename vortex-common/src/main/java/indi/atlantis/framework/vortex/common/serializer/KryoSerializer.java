/**
* Copyright 2021 Fred Feng (paganini.fy@gmail.com)

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

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.util.Pool;

import indi.atlantis.framework.vortex.common.Tuple;
import indi.atlantis.framework.vortex.common.TupleImpl;

/**
 * 
 * KryoSerializer
 * 
 * @author Fred Feng
 * @version 1.0
 */
public class KryoSerializer implements Serializer {

	public static final int DEFAULT_POOL_SIZE = 16;
	public static final int DEFAULT_IO_POOL_SIZE = 128;

	private final Pool<Kryo> pool;
	private final Pool<Output> outputPool;
	private final Pool<Input> inputPool;

	public KryoSerializer() {
		this(DEFAULT_POOL_SIZE, DEFAULT_IO_POOL_SIZE, DEFAULT_IO_POOL_SIZE, 8192);
	}

	public KryoSerializer(int poolSize, int outputSize, int inputSize, int bufferSize) {
		this.pool = KryoUtils.getPool(poolSize);
		this.outputPool = KryoUtils.getOutputPool(outputSize, bufferSize);
		this.inputPool = KryoUtils.getInputPool(inputSize, bufferSize);
	}

	public byte[] serialize(Tuple tuple) {
		Kryo kryo = pool.obtain();
		Output output = outputPool.obtain();
		try {
			output.reset();
			kryo.writeObject(output, tuple);
			return output.getBuffer();
		} finally {
			outputPool.free(output);
			pool.free(kryo);
		}
	}

	public Tuple deserialize(byte[] bytes) {
		Kryo kryo = pool.obtain();
		Input input = inputPool.obtain();
		try {
			input.setBuffer(bytes);
			Tuple tuple = kryo.readObject(input, TupleImpl.class);
			tuple.setLength(bytes.length);
			return tuple;
		} finally {
			inputPool.free(input);
			pool.free(kryo);
		}
	}

}
