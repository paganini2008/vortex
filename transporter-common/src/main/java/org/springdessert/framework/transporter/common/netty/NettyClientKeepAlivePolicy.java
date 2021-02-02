package org.springdessert.framework.transporter.common.netty;

import org.springdessert.framework.transporter.common.Tuple;

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
