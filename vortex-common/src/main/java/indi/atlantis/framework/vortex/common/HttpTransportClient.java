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

import java.util.function.Function;

/**
 * 
 * HttpTransportClient
 *
 * @author Fred Feng
 * @since 2.0.1
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
