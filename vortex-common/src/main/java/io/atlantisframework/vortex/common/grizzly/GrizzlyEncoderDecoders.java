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
package io.atlantisframework.vortex.common.grizzly;

import org.glassfish.grizzly.AbstractTransformer;
import org.glassfish.grizzly.Buffer;
import org.glassfish.grizzly.TransformationException;
import org.glassfish.grizzly.TransformationResult;
import org.glassfish.grizzly.attributes.AttributeStorage;

import io.atlantisframework.vortex.common.TransportClientException;
import io.atlantisframework.vortex.common.Tuple;
import io.atlantisframework.vortex.common.serializer.Serializer;

/**
 * 
 * GrizzlyEncoderDecoders
 *
 * @author Fred Feng
 * @since 2.0.1
 */
public abstract class GrizzlyEncoderDecoders {

	public static class TupleDecoder extends AbstractTransformer<Buffer, Tuple> {

		private final Serializer serializer;

		public TupleDecoder(Serializer serializer) {
			this.serializer = serializer;
		}

		@Override
		public String getName() {
			return "TupleDecoder";
		}

		@Override
		public boolean hasInputRemaining(AttributeStorage storage, Buffer input) {
			return input != null && input.hasRemaining();
		}

		@Override
		protected TransformationResult<Buffer, Tuple> transformImpl(AttributeStorage storage, Buffer input) throws TransformationException {
			Integer objectSize = (Integer) storage.getAttributes().getAttribute("objectSize");
			if (objectSize == null) {
				if (input.remaining() < 4) {
					return TransformationResult.createIncompletedResult(input);
				}
				objectSize = input.getInt();
				storage.getAttributes().setAttribute("objectSize", objectSize);
			}
			if (input.remaining() < objectSize) {
				return TransformationResult.createIncompletedResult(input);
			}
			final int limit = input.limit();
			input.limit(input.position() + objectSize);
			byte[] data = new byte[input.remaining()];
			input.get(data);
			Tuple tuple = serializer.deserialize(data);
			
			input.position(input.limit());
			input.limit(limit);
			storage.getAttributes().removeAttribute("objectSize");
			return TransformationResult.createCompletedResult(tuple, input);
		}

	}

	public static class TupleEncoder extends AbstractTransformer<Tuple, Buffer> {

		private final Serializer serializer;

		public TupleEncoder(Serializer serializer) {
			this.serializer = serializer;
		}

		@Override
		public String getName() {
			return "TupleEncoder";
		}

		@Override
		public boolean hasInputRemaining(AttributeStorage storage, Tuple input) {
			return input != null;
		}

		@Override
		protected TransformationResult<Tuple, Buffer> transformImpl(AttributeStorage storage, Tuple input) throws TransformationException {
			if (input == null) {
				throw new TransportClientException("Input could not be null");
			}
			byte[] data = serializer.serialize(input);

			final Buffer output = obtainMemoryManager(storage).allocate(data.length + 4);
			output.putInt(data.length);
			output.put(data);
			output.flip();
			output.allowBufferDispose(true);

			return TransformationResult.createCompletedResult(output, null);
		}

	}

}
