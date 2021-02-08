package indi.atlantis.framework.vortex.common.netty;

import indi.atlantis.framework.vortex.common.Tuple;
import io.netty.channel.ChannelHandlerContext;

/**
 * 
 * NettyClientKeepAlivePolicy
 *
 * @author Jimmy Hoff
 * @version 1.0
 */
public class NettyClientKeepAlivePolicy extends KeepAlivePolicy {

	protected void whenWriterIdle(ChannelHandlerContext ctx) {
		ctx.channel().writeAndFlush(Tuple.PING);
	}
	
}
