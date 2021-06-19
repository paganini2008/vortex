/**
* Copyright 2021 Fred Feng (paganini.fy@gmail.com)

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
package indi.atlantis.framework.vortex.common;

import java.net.SocketAddress;

/**
 * 
 * HttpClient
 * 
 * @author Fred Feng
 *
 * @since 1.0
 */
public interface HttpClient extends LifeCycle, Client {

	void addHeader(String name, String value);

	default void setHeader(String name, String value) {
		addHeader(name, value);
	}

	default void send(SocketAddress address, Object data) {
		send(data);
	}

	default void send(Object data, Partitioner partitioner) {
		send(data);
	}

}
