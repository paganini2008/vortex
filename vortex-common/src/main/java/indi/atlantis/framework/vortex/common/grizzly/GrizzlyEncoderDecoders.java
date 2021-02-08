package indi.atlantis.framework.vortex.common.grizzly;

import org.glassfish.grizzly.AbstractTransformer;
import org.glassfish.grizzly.Buffer;
import org.glassfish.grizzly.TransformationException;
import org.glassfish.grizzly.TransformationResult;
import org.glassfish.grizzly.attributes.AttributeStorage;

import indi.atlantis.framework.vortex.common.TransportClientException;
import indi.atlantis.framework.vortex.common.Tuple;
import indi.atlantis.framework.vortex.common.serializer.Serializer;

/**
 * 
 * GrizzlyEncoderDecoders
 *
 * @author Jimmy Hoff
 * @version 1.0
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
