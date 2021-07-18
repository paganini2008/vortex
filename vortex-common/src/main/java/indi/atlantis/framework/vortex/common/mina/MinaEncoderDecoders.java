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
package indi.atlantis.framework.vortex.common.mina;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.CumulativeProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.apache.mina.filter.codec.ProtocolEncoderAdapter;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;

import indi.atlantis.framework.vortex.common.TransportClientException;
import indi.atlantis.framework.vortex.common.Tuple;
import indi.atlantis.framework.vortex.common.serializer.Serializer;

/**
 * 
 * MinaEncoderDecoders
 * 
 * @author Fred Feng
 * @version 1.0
 */
public abstract class MinaEncoderDecoders {

	public static class TupleEncoder extends ProtocolEncoderAdapter {
		private final Serializer serializer;
		private int maxObjectSize = Integer.MAX_VALUE;

		public TupleEncoder(Serializer serializer) {
			this.serializer = serializer;
		}

		public int getMaxObjectSize() {
			return maxObjectSize;
		}

		public void setMaxObjectSize(int maxObjectSize) {
			if (maxObjectSize <= 0) {
				throw new IllegalArgumentException("maxObjectSize: " + maxObjectSize);
			}
			this.maxObjectSize = maxObjectSize;
		}

		@Override
		public void encode(IoSession session, Object message, ProtocolEncoderOutput out) throws Exception {
			if (!(message instanceof Tuple)) {
				return;
			}
			byte[] data = serializer.serialize((Tuple) message);
			IoBuffer buf = IoBuffer.allocate(128).setAutoExpand(true);
			buf.putInt(data.length);
			buf.put(data);

			int objectSize = buf.position() - 4;
			if (objectSize > maxObjectSize) {
				throw new IllegalArgumentException("The encoded object is too big: " + objectSize + " (> " + maxObjectSize + ')');
			}
			buf.flip();
			out.write(buf);
		}
	}

	public static class TupleDecoder extends CumulativeProtocolDecoder {

		private final Serializer serializer;
		private int maxObjectSize = Integer.MAX_VALUE;

		public TupleDecoder(Serializer serializer) {
			this.serializer = serializer;
		}

		public int getMaxObjectSize() {
			return maxObjectSize;
		}

		public void setMaxObjectSize(int maxObjectSize) {
			if (maxObjectSize <= 0) {
				throw new IllegalArgumentException("maxObjectSize: " + maxObjectSize);
			}
			this.maxObjectSize = maxObjectSize;
		}

		@Override
		protected boolean doDecode(IoSession session, IoBuffer in, ProtocolDecoderOutput out) throws Exception {
			if (!in.prefixedDataAvailable(4, maxObjectSize)) {
				return false;
			}
			int dataLength = in.getInt();
			if (dataLength < 4) {
				throw new TransportClientException("Data length should be greater than 4: " + dataLength);
			}
			byte[] data = new byte[dataLength];
			in.get(data);
			Tuple tuple = serializer.deserialize(data);
			out.write(tuple);
			return true;
		}

	}

}
