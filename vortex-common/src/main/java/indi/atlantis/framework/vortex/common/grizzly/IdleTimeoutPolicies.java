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
