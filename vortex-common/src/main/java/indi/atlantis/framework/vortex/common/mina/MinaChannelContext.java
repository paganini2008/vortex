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
package indi.atlantis.framework.vortex.common.mina;

import java.net.SocketAddress;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.mina.core.session.IoSession;

import indi.atlantis.framework.vortex.common.ChannelContext;
import indi.atlantis.framework.vortex.common.Partitioner;

/**
 * 
 * MinaChannelContext
 * 
 * @author Fred Feng
 * 
 * 
 * @version 1.0
 */
public class MinaChannelContext extends MinaChannelContextAware implements ChannelContext<IoSession> {

	private final List<IoSession> holder = new CopyOnWriteArrayList<IoSession>();

	public void addChannel(IoSession channel, int weight) {
		for (int i = 0; i < weight; i++) {
			holder.add(channel);
		}
	}

	public IoSession getChannel(SocketAddress address) {
		for (IoSession channel : holder) {
			if (channel.getRemoteAddress() != null && channel.getRemoteAddress().equals(address)) {
				return channel;
			}
		}
		return null;
	}

	public void removeChannel(SocketAddress address) {
		for (IoSession channel : holder) {
			if (channel.getRemoteAddress() != null && channel.getRemoteAddress().equals(address)) {
				holder.remove(channel);
			}
		}
	}

	public int countOfChannels() {
		return holder.size();
	}

	public IoSession selectChannel(Object data, Partitioner partitioner) {
		return holder.isEmpty() ? null : partitioner.selectChannel(data, holder);
	}

	public Collection<IoSession> getChannels() {
		return holder;
	}

}
