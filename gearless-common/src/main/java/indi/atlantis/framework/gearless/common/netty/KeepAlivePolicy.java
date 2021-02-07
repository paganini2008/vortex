package indi.atlantis.framework.gearless.common.netty;

import io.netty.channel.ChannelHandler.Sharable;
import indi.atlantis.framework.gearless.common.KeepAliveTimeoutException;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleStateEvent;

/**
 * 
 * KeepAlivePolicy
 *
 * @author Jimmy Hoff
 * @version 1.0
 */
@Sharable
public abstract class KeepAlivePolicy extends ChannelInboundHandlerAdapter {

	public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
		if (evt instanceof IdleStateEvent) {
			IdleStateEvent e = (IdleStateEvent) evt;
			switch (e.state()) {
			case READER_IDLE:
				whenReaderIdle(ctx);
				break;
			case WRITER_IDLE:
				whenWriterIdle(ctx);
				break;
			case ALL_IDLE:
				whenBothIdle(ctx);
				break;
			}
		} else {
			super.userEventTriggered(ctx, evt);
		}
	}

	protected void whenReaderIdle(ChannelHandlerContext ctx) {
		throw new KeepAliveTimeoutException("Reading Idle.");
	}

	protected void whenWriterIdle(ChannelHandlerContext ctx) {
		throw new KeepAliveTimeoutException("Writing Idle.");
	}

	protected void whenBothIdle(ChannelHandlerContext ctx) {
		throw new KeepAliveTimeoutException("Reading or Writing Idle.");
	}

}
