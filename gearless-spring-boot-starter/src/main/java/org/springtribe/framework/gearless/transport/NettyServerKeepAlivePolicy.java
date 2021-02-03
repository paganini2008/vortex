package org.springtribe.framework.gearless.transport;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springtribe.framework.gearless.common.ChannelEvent;
import org.springtribe.framework.gearless.common.ChannelEventListener;
import org.springtribe.framework.gearless.common.Tuple;
import org.springtribe.framework.gearless.common.ChannelEvent.EventType;
import org.springtribe.framework.gearless.common.netty.KeepAlivePolicy;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

/**
 * 
 * NettyServerKeepAlivePolicy
 *
 * @author Jimmy Hoff
 * @version 1.0
 */
@Slf4j
public class NettyServerKeepAlivePolicy extends KeepAlivePolicy {

	@Value("${spring.application.cluster.transport.nioserver.keepalive.response:true}")
	private boolean keepaliveResposne;

	@Value("${spring.application.cluster.transport.nioserver.idleTimeout:60}")
	private int idleTimeout;

	@Autowired(required = false)
	private ChannelEventListener<Channel> channelEventListener;

	@Override
	protected void whenReaderIdle(ChannelHandlerContext ctx) {
		if (log.isTraceEnabled()) {
			log.trace("A keep-alive message was not received within {} second(s).", idleTimeout);
		}
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object data) throws Exception {
		if (isPing(data)) {
			if (channelEventListener != null) {
				channelEventListener.fireChannelEvent(new ChannelEvent<Channel>(ctx.channel(), EventType.PING, null));
			}
			if (keepaliveResposne) {
				ctx.writeAndFlush(Tuple.PONG);
			}
		} else {
			ctx.fireChannelRead(data);
		}
	}

	protected boolean isPing(Object data) {
		return (data instanceof Tuple) && ((Tuple) data).isPing();
	}

}
