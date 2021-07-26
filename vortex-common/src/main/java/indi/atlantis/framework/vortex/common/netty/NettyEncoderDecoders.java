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
package indi.atlantis.framework.vortex.common.netty;

import java.util.List;

import indi.atlantis.framework.vortex.common.TransportClientException;
import indi.atlantis.framework.vortex.common.Tuple;
import indi.atlantis.framework.vortex.common.serializer.Serializer;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 * 
 * NettyEncoderDecoders
 * 
 * @author Fred Feng
 * @since 2.0.1
 */
public abstract class NettyEncoderDecoders {

	public static class TupleEncoder extends MessageToByteEncoder<Tuple> {

		private final Serializer serializer;

		public TupleEncoder(Serializer serializer) {
			this.serializer = serializer;
		}

		@Override
		protected void encode(ChannelHandlerContext ctx, Tuple input, ByteBuf out) throws Exception {
			if (input == null) {
				throw new TransportClientException("Input could not be null");
			}
			byte[] data = serializer.serialize(input);
			out.writeInt(data.length);
			out.writeBytes(data);
		}

	}

	public static class TupleDecoder extends ByteToMessageDecoder {

		private final Serializer serializer;

		public TupleDecoder(Serializer serializer) {
			this.serializer = serializer;
		}

		@Override
		protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
			if (in.readableBytes() < 4) {
				return;
			}
			in.markReaderIndex();
			int dataLength = in.readInt();
			if (dataLength < 4) {
				throw new TransportClientException("Data length should be greater than 4: " + dataLength);
			}
			if (in.readableBytes() < dataLength) {
				in.resetReaderIndex();
				return;
			}

			byte[] body = new byte[dataLength];
			in.readBytes(body);
			Tuple tuple = serializer.deserialize(body);
			out.add(tuple);
		}

	}

}
