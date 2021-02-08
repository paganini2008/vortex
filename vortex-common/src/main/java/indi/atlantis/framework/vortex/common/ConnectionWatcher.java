package indi.atlantis.framework.vortex.common;

import java.net.SocketAddress;
import java.util.concurrent.TimeUnit;

import com.github.paganini2008.devtools.Observable;
import com.github.paganini2008.devtools.multithreads.ThreadUtils;

/**
 * 
 * ConnectionWatcher
 *
 * @author Jimmy Hoff
 * @version 1.0
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
