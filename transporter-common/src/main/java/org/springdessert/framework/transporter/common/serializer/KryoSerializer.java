package org.springdessert.framework.transporter.common.serializer;

import org.springdessert.framework.transporter.common.Tuple;
import org.springdessert.framework.transporter.common.TupleImpl;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.util.Pool;

/**
 * 
 * KryoSerializer
 * 
 * @author Jimmy Hoff
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
			return kryo.readObject(input, TupleImpl.class);
		} finally {
			inputPool.free(input);
			pool.free(kryo);
		}
	}

}
