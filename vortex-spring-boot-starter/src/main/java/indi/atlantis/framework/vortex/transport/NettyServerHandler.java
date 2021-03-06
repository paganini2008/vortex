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
 * @author Jimmy Hoff
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
