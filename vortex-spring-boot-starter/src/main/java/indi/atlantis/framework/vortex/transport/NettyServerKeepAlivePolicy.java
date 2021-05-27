package indi.atlantis.framework.vortex.transport;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import indi.atlantis.framework.vortex.common.ChannelEvent;
import indi.atlantis.framework.vortex.common.ChannelEventListener;
import indi.atlantis.framework.vortex.common.Tuple;
import indi.atlantis.framework.vortex.common.ChannelEvent.EventType;
import indi.atlantis.framework.vortex.common.netty.KeepAlivePolicy;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

/**
 * 
 * NettyServerKeepAlivePolicy
 *
 * @author Fred Feng
 * @version 1.0
 */
@Slf4j
public class NettyServerKeepAlivePolicy extends KeepAlivePolicy {

	@Value("${atlantis.framework.vortex.nioserver.keepalive.response:true}")
	private boolean keepaliveResposne;

	@Value("${atlantis.framework.vortex.nioserver.idleTimeout:60}")
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
