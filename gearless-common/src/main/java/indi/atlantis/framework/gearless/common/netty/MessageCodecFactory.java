package indi.atlantis.framework.gearless.common.netty;

import io.netty.channel.ChannelHandler;

/**
 * 
 * MessageCodecFactory
 *
 * @author Jimmy Hoff
 * @version 1.0
 */
public interface MessageCodecFactory {

	ChannelHandler getEncoder();

	ChannelHandler getDecoder();

}