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

import java.util.concurrent.TimeUnit;

import org.glassfish.grizzly.Connection;

import indi.atlantis.framework.vortex.common.KeepAliveTimeoutException;
import indi.atlantis.framework.vortex.common.Tuple;
import lombok.extern.slf4j.Slf4j;

/**
 * 
 * IdleTimeoutPolicies
 * 
 * @author Fred Feng
 * @version 1.0
 */
@Slf4j
@SuppressWarnings("all")
public abstract class IdleTimeoutPolicies {

	public static IdleTimeoutFilter.TimeoutHandler PING = new IdleTimeoutFilter.TimeoutHandler() {

		public void onTimeout(Connection connection) {
			connection.write(Tuple.PING);
			throw new KeepAliveTimeoutException();
		}

	};

	public static IdleTimeoutFilter.TimeoutHandler READER_IDLE_LOG = new IdleTimeoutFilter.TimeoutHandler() {

		public void onTimeout(Connection connection) {
			log.warn("[Reader Idle] Send a keep-alive message after {} second(s).", connection.getReadTimeout(TimeUnit.SECONDS));
			throw new KeepAliveTimeoutException();
		}

	};

	public static IdleTimeoutFilter.TimeoutHandler WRITER_IDLE_LOG = new IdleTimeoutFilter.TimeoutHandler() {

		public void onTimeout(Connection connection) {
			log.warn("[Writer Idle] Send a keep-alive message after {} second(s).", connection.getWriteTimeout(TimeUnit.SECONDS));
			throw new KeepAliveTimeoutException();
		}

	};

	public static IdleTimeoutFilter.TimeoutHandler CLOSE = new IdleTimeoutFilter.TimeoutHandler() {

		public void onTimeout(Connection connection) {
		}

	};

}
