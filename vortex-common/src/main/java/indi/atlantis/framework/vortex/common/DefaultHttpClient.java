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

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.paganini2008.devtools.collection.LruQueue;
import com.github.paganini2008.devtools.collection.MapUtils;
import com.github.paganini2008.devtools.logging.Log;
import com.github.paganini2008.devtools.logging.LogFactory;
import com.github.paganini2008.devtools.multithreads.Executable;
import com.github.paganini2008.devtools.multithreads.ThreadUtils;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.ConnectionPool;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * 
 * DefaultHttpClient
 * 
 * @author Fred Feng
 *
 * @since 2.0.1
 */
public class DefaultHttpClient implements HttpClient, Executable {

	private static final Log logger = LogFactory.getLog(DefaultHttpClient.class);
	private static final String servicePath = "/application/cluster/transport/emit";
	private static final int DEFAULT_TIMEOUT = 60;
	private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
	private static final int RETRY_QUEUE_MAX_SIZE = 1024;
	private final Queue<Object> retryQueue = new LruQueue<Object>(RETRY_QUEUE_MAX_SIZE);

	private ObjectMapper objectMapper = new ObjectMapper();

	public DefaultHttpClient(String brokerUrl) {
		this(brokerUrl, () -> {
			return new OkHttpClient.Builder().retryOnConnectionFailure(false).connectionPool(new ConnectionPool(200, 60, TimeUnit.SECONDS))
					.connectTimeout(Duration.ofSeconds(DEFAULT_TIMEOUT)).writeTimeout(Duration.ofSeconds(DEFAULT_TIMEOUT))
					.readTimeout(Duration.ofSeconds(DEFAULT_TIMEOUT)).build();
		});
	}

	public DefaultHttpClient(String brokerUrl, Supplier<OkHttpClient> supplier) {
		this.brokerUrl = brokerUrl;
		this.client = supplier.get();
		this.active.set(true);
		ThreadUtils.scheduleWithFixedDelay(this, 5, TimeUnit.SECONDS);
	}

	private final String brokerUrl;
	private final OkHttpClient client;
	private final Map<String, String> defaultHeaders = new LinkedHashMap<String, String>();
	private final AtomicBoolean active = new AtomicBoolean();

	public void setObjectMapper(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	@Override
	public void addHeader(String name, String value) {
		defaultHeaders.putIfAbsent(name, value);
	}

	@Override
	public void setHeader(String name, String value) {
		defaultHeaders.put(name, value);
	}

	@Override
	public void send(Object data) {
		String jsonString;
		try {
			jsonString = objectMapper.writeValueAsString(data);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			return;
		}
		Request.Builder requestBuilder = new Request.Builder().url(brokerUrl + servicePath).post(RequestBody.create(JSON, jsonString));
		if (MapUtils.isNotEmpty(defaultHeaders)) {
			requestBuilder.headers(Headers.of(defaultHeaders));
		}

		Request request = requestBuilder.build();
		Call call = client.newCall(request);
		call.enqueue(new Callback() {

			@Override
			public void onResponse(Call call, Response response) throws IOException {
				if (response.code() >= 200 && response.code() < 300) {
					if (logger.isDebugEnabled()) {
						logger.debug(response.body().string());
					}
				}
				response.close();
			}

			@Override
			public void onFailure(Call call, IOException e) {
				if (logger.isErrorEnabled()) {
					logger.error(e.getMessage(), e);
				}
				retryQueue.add(data);
			}
		});

	}

	@Override
	public boolean isOpened() {
		return active.get();
	}

	@Override
	public void close() {
		active.set(false);
		client.connectionPool().evictAll();
	}

	@Override
	public boolean execute() {
		if (retryQueue.size() > 0) {
			ArrayDeque<Object> q = new ArrayDeque<Object>(retryQueue);
			while (!q.isEmpty()) {
				Object data = q.poll();
				retryQueue.remove(data);
				send(data);
			}
		}
		return isOpened();
	}

}
