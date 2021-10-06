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
package io.atlantisframework.vortex.transport;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import com.github.paganini2008.embeddedio.Channel;
import com.github.paganini2008.embeddedio.ChannelHandler;
import com.github.paganini2008.embeddedio.MessagePacket;

import io.atlantisframework.vortex.Accumulator;
import io.atlantisframework.vortex.buffer.BufferZone;
import io.atlantisframework.vortex.common.ChannelEvent;
import io.atlantisframework.vortex.common.ChannelEventListener;
import io.atlantisframework.vortex.common.Tuple;
import io.atlantisframework.vortex.common.ChannelEvent.EventType;
import lombok.extern.slf4j.Slf4j;

/**
 * 
 * EmbeddedServerHandler
 *
 * @author Fred Feng
 * @since 2.0.1
 */
@Slf4j
public class EmbeddedServerHandler implements ChannelHandler {

	@Autowired
	private BufferZone store;

	@Autowired
	private Accumulator accumulator;

	@Value("${atlantis.framework.vortex.bufferzone.collectionName}")
	private String collectionName;

	@Value("${atlantis.framework.vortex.nioserver.keepalive.response:true}")
	private boolean keepaliveResposne;

	@Autowired(required = false)
	private ChannelEventListener<Channel> channelEventListener;

	@Override
	public void fireChannelActive(Channel channel) throws IOException {
		fireChannelEvent(channel, EventType.CONNECTED, null);
	}

	@Override
	public void fireChannelInactive(Channel channel) throws IOException {
		fireChannelEvent(channel, EventType.CLOSED, null);
	}

	@Override
	public void fireChannelReadable(Channel channel, MessagePacket packet) throws Exception {
		if (isPing(packet.getMessage())) {
			if (channelEventListener != null) {
				channelEventListener.fireChannelEvent(new ChannelEvent<Channel>(channel, EventType.PING, null));
			}
			if (keepaliveResposne) {
				channel.writeAndFlush(Tuple.PONG);
			}
		} else {
			for (Object message : packet.getMessages()) {
				store.set(collectionName, (Tuple) message);
				accumulator.accumulate((Tuple) message);
			}
		}
	}

	@Override
	public void fireChannelFatal(Channel channel, Throwable e) {
		log.error(e.getMessage(), e);
		fireChannelEvent(channel, EventType.FAULTY, e);
	}

	protected boolean isPing(Object data) {
		return (data instanceof Tuple) && ((Tuple) data).isPing();
	}

	private void fireChannelEvent(Channel channel, EventType eventType, Throwable cause) {
		if (channelEventListener != null) {
			channelEventListener.fireChannelEvent(new ChannelEvent<Channel>(channel, eventType, cause));
		}
	}

}
