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

import org.objenesis.strategy.StdInstantiatorStrategy;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.util.Pool;

/**
 * 
 * KryoUtils
 *
 * @author Fred Feng
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
