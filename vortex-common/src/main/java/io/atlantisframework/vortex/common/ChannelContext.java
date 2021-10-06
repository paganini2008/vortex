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
package io.atlantisframework.vortex.common;

import java.net.SocketAddress;
import java.util.Collection;

/**
 * 
 * ChannelContext
 * 
 * @author Fred Feng
 * @since 2.0.1
 */
public interface ChannelContext<T> {

	default void addChannel(T channel) {
		addChannel(channel, 1);
	}

	void addChannel(T channel, int weight);

	T getChannel(SocketAddress address);

	void removeChannel(SocketAddress address);

	int countOfChannels();

	T selectChannel(Object data, Partitioner partitioner);

	Collection<T> getChannels();

	void setChannelEventListener(ChannelEventListener<T> channelEventListener);

}