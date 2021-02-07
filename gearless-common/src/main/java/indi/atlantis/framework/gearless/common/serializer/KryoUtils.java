package indi.atlantis.framework.gearless.common.serializer;

import org.objenesis.strategy.StdInstantiatorStrategy;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.util.Pool;

/**
 * 
 * KryoUtils
 *
 * @author Jimmy Hoff
 * @version 1.0
 */
public abstract class KryoUtils {

	public static Pool<Kryo> getPool(int poolSize) {
		return new Pool<Kryo>(true, false, poolSize) {

			@Override
			protected Kryo create() {
				Kryo kryo = new Kryo();
				kryo.setReferences(false);
				kryo.setRegistrationRequired(false);
				kryo.setInstantiatorStrategy(new StdInstantiatorStrategy());
				return kryo;
			}

		};
	}

	public static Pool<Output> getOutputPool(int poolSize, int bufferSize) {
		return new Pool<Output>(true, false, poolSize) {
			protected Output create() {
				return new Output(bufferSize, -1);
			}
		};
	}

	public static Pool<Input> getInputPool(int poolSize, int bufferSize) {
		return new Pool<Input>(true, false, poolSize) {
			protected Input create() {
				return new Input(bufferSize);
			}
		};
	}

}
