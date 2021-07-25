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
package indi.atlantis.framework.vortex.transport;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import indi.atlantis.framework.vortex.Accumulator;
import indi.atlantis.framework.vortex.buffer.BufferZone;
import indi.atlantis.framework.vortex.common.ChannelEvent;
import indi.atlantis.framework.vortex.common.ChannelEvent.EventType;
import indi.atlantis.framework.vortex.common.ChannelEventListener;
import indi.atlantis.framework.vortex.common.Tuple;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.slf4j.Slf4j;

/**
 * 
 * NettyServerHandler
 * 
 * @author Fred Feng
 * @version 1.0
 */
@Slf4j
@Sharable
public class NettyServerHandler extends ChannelInboundHandlerAdapter {

	@Autowired
	private BufferZone bufferZone;

	@Autowired
	private Accumulator accumulator;

	@Autowired(required = false)
	private ChannelEventListener<Channel> channelEventListener;

	@Value("${atlantis.framework.vortex.bufferzone.collectionName}")
	private String collectionName;

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		super.channelActive(ctx);
		fireChannelEvent(ctx.channel(), EventType.CONNECTED, null);
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		super.channelInactive(ctx);
		fireChannelEvent(ctx.channel(), EventType.CLOSED, null);
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		super.exceptionCaught(ctx, cause);
		log.error(cause.getMessage(), cause);
		fireChannelEvent(ctx.channel(), EventType.FAULTY, cause);
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object message) throws Exception {
		bufferZone.set(collectionName, (Tuple) message);
		accumulator.accumulate((Tuple) message);
	}

	private void fireChannelEvent(Channel channel, EventType eventType, Throwable cause) {
		if (channelEventListener != null) {
			channelEventListener.fireChannelEvent(new ChannelEvent<Channel>(channel, eventType, cause));
		}
	}

}
