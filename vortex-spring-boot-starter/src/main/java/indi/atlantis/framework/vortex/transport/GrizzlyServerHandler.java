package indi.atlantis.framework.vortex.transport;

import java.io.IOException;

import org.glassfish.grizzly.Connection;
import org.glassfish.grizzly.filterchain.BaseFilter;
import org.glassfish.grizzly.filterchain.FilterChainContext;
import org.glassfish.grizzly.filterchain.NextAction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import indi.atlantis.framework.vortex.Accumulator;
import indi.atlantis.framework.vortex.buffer.BufferZone;
import indi.atlantis.framework.vortex.common.ChannelEvent;
import indi.atlantis.framework.vortex.common.ChannelEvent.EventType;
import indi.atlantis.framework.vortex.common.ChannelEventListener;
import indi.atlantis.framework.vortex.common.Tuple;
import lombok.extern.slf4j.Slf4j;

/**
 * 
 * GrizzlyServerHandler
 *
 * @author Jimmy Hoff
 * @version 1.0
 */
@Slf4j
public class GrizzlyServerHandler extends BaseFilter {

	@Value("${atlantis.framework.vortex.nioserver.keepalive.response:true}")
	private boolean keepaliveResposne;

	@Autowired
	private BufferZone bufferZone;

	@Autowired
	private Accumulator accumulator;

	@Value("${atlantis.framework.vortex.bufferzone.collectionName}")
	private String collectionName;

	@Autowired(required = false)
	private ChannelEventListener<Connection<?>> channelEventListener;

	@Override
	public NextAction handleRead(FilterChainContext ctx) throws IOException {
		Tuple message = ctx.getMessage();
		if (isPing(message)) {
			if (channelEventListener != null) {
				channelEventListener.fireChannelEvent(new ChannelEvent<Connection<?>>(ctx.getConnection(), EventType.PING, null));
			}
			if (keepaliveResposne) {
				ctx.write(Tuple.PONG);
			}
			return ctx.getStopAction();
		} else {
			try {
				bufferZone.set(collectionName, message);
				accumulator.accumulate(message);
			} catch (Exception e) {
				if (e instanceof IOException) {
					throw (IOException) e;
				}
				throw new IOException(e);
			}
			return ctx.getInvokeAction();
		}
	}

	protected boolean isPing(Object data) {
		return (data instanceof Tuple) && ((Tuple) data).isPing();
	}

	@Override
	public NextAction handleAccept(FilterChainContext ctx) throws IOException {
		fireChannelEvent(ctx.getConnection(), EventType.CONNECTED, null);
		return ctx.getInvokeAction();
	}

	@Override
	public NextAction handleClose(FilterChainContext ctx) throws IOException {
		fireChannelEvent(ctx.getConnection(), EventType.CLOSED, null);
		return ctx.getInvokeAction();
	}

	@Override
	public void exceptionOccurred(FilterChainContext ctx, Throwable cause) {
		log.error(cause.getMessage(), cause);
		fireChannelEvent(ctx.getConnection(), EventType.FAULTY, cause);
	}

	private void fireChannelEvent(Connection<?> channel, EventType eventType, Throwable cause) {
		if (channelEventListener != null) {
			channelEventListener.fireChannelEvent(new ChannelEvent<Connection<?>>(channel, eventType, cause));
		}
	}

}
