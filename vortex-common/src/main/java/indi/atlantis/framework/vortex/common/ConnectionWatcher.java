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
package indi.atlantis.framework.vortex.common;

import java.net.SocketAddress;
import java.util.concurrent.TimeUnit;

import com.github.paganini2008.devtools.Observable;
import com.github.paganini2008.devtools.multithreads.ThreadUtils;

/**
 * 
 * ConnectionWatcher
 *
 * @author Fred Feng
 * @since 2.0.1
 */
public final class ConnectionWatcher {

	private final int checkInterval;
	private final TimeUnit timeUnit;
	private final NioConnection connection;

	public ConnectionWatcher(int checkInterval, TimeUnit timeUnit, NioConnection connection) {
		this.checkInterval = checkInterval;
		this.timeUnit = timeUnit;
		this.connection = connection;
	}

	private final Observable observable = Observable.unrepeatable();

	public void reconnect(SocketAddress remoteAddress) {
		observable.notifyObservers(remoteAddress);
	}

	public void watch(final SocketAddress remoteAddress, final HandshakeCallback callback) {
		observable.addObserver((ob, arg) -> {
			do {
				ThreadUtils.sleep(checkInterval, timeUnit);
				connection.connect(remoteAddress, callback);
			} while (!connection.isConnected(remoteAddress));
		});
	}

}
