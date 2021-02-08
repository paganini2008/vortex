package indi.atlantis.framework.vortex.common;

import java.util.function.Function;

/**
 * 
 * HttpTransportClient
 *
 * @author Jimmy Hoff
 * @version 1.0
 */
public class HttpTransportClient implements TransportClient {

	public HttpTransportClient(String brokerUrl) {
		this(brokerUrl, url -> {
			return new DefaultHttpClient(brokerUrl);
		});
	}

	public HttpTransportClient(String brokerUrl, Function<String, HttpClient> supplier) {
		this.client = supplier.apply(brokerUrl);
	}

	private HttpClient client;

	@Override
	public void write(Tuple tuple) {
		if (isActive()) {
			client.send(tuple);
		}
	}

	@Override
	public boolean isActive() {
		return client.isOpened();
	}

	@Override
	public void close() {
		client.close();
	}

}
