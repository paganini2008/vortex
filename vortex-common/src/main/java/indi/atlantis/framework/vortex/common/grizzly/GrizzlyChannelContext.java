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
package indi.atlantis.framework.vortex.common.grizzly;

import java.net.SocketAddress;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.glassfish.grizzly.Connection;

import indi.atlantis.framework.vortex.common.ChannelContext;
import indi.atlantis.framework.vortex.common.Partitioner;

/**
 * 
 * GrizzlyChannelContext
 *
 * @author Fred Feng
 * @since 2.0.1
 */
public class GrizzlyChannelContext extends GrizzlyChannelContextAware implements ChannelContext<Connection<?>> {

	private final List<Connection<?>> holder = new CopyOnWriteArrayList<Connection<?>>();

	public void addChannel(Connection<?> channel, int weight) {
		for (int i = 0; i < weight; i++) {
			holder.add(channel);
		}
	}

	public Connection<?> getChannel(SocketAddress address) {
		for (Connection<?> channel : holder) {
			if (getRemoteAddress(channel) != null && getRemoteAddress(channel).equals(address)) {
				return channel;
			}
		}
		return null;
	}

	public void removeChannel(SocketAddress address) {
		for (Connection<?> channel : holder) {
			if (getRemoteAddress(channel) != null && getRemoteAddress(channel).equals(address)) {
				holder.remove(channel);
			}
		}
	}

	private SocketAddress getRemoteAddress(Connection<?> channel) {
		return (SocketAddress) channel.getPeerAddress();
	}

	public int countOfChannels() {
		return holder.size();
	}

	public Connection<?> selectChannel(Object data, Partitioner partitioner) {
		return holder.isEmpty() ? null : partitioner.selectChannel(data, holder);
	}

	public Collection<Connection<?>> getChannels() {
		return holder;
	}
}
