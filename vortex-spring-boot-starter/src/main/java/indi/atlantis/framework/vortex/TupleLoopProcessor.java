/**
* Copyright 2018-2021 Fred Feng (paganini.fy@gmail.com)

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
package indi.atlantis.framework.vortex;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import com.github.paganini2008.devtools.Assert;
import com.github.paganini2008.devtools.collection.CollectionUtils;
import com.github.paganini2008.devtools.collection.MapUtils;
import com.github.paganini2008.devtools.multithreads.ExecutorUtils;
import com.github.paganini2008.devtools.multithreads.ThreadUtils;

import indi.atlantis.framework.vortex.buffer.BufferZone;
import indi.atlantis.framework.vortex.common.Tuple;
import lombok.extern.slf4j.Slf4j;

/**
 * 
 * TupleLoopProcessor
 * 
 * @author Fred Feng
 *
 * @since 1.0
 */
@Slf4j
public class TupleLoopProcessor
		implements Runnable, ApplicationListener<ContextRefreshedEvent>, BeanPostProcessor, DisposableBean {

	@Autowired
	private BufferZone bufferZone;

	@Qualifier("loopProcessorThreadPool")
	@Autowired(required = false)
	private ThreadPoolTaskExecutor threadPool;

	@Value("${atlantis.framework.vortex.bufferzone.collectionName}")
	private String collectionName;

	@Value("${atlantis.framework.vortex.bufferzone.pullSize:1}")
	private int pullSize;

	private final List<BulkHandler> bulkHandlers = new CopyOnWriteArrayList<BulkHandler>();
	private final Map<String, List<Handler>> topicHandlers = new ConcurrentHashMap<String, List<Handler>>();
	private final AtomicBoolean running = new AtomicBoolean(false);
	private Thread runner;

	public void addHandler(Handler handler) {
		Assert.isNull(handler, "Nullable handler");
		List<Handler> handlers = MapUtils.get(topicHandlers, handler.getTopic(), () -> {
			return new CopyOnWriteArrayList<Handler>();
		});
		handlers.add(handler);
		log.info("Add handler: {}/{}", handler.getTopic(), handler);
	}

	public void removeHandler(Handler handler) {
		Assert.isNull(handler, "Nullable handler");
		List<Handler> handlers = topicHandlers.get(handler.getTopic());
		if (handlers != null) {
			while (handlers.contains(handler)) {
				handlers.remove(handler);
			}
			log.info("Remove handler: {}/{}", handler.getTopic(), handler);
		}
	}

	public void addHandler(BulkHandler handler) {
		Assert.isNull(handler, "Nullable handler");
		bulkHandlers.add(handler);
	}

	public void removeHandler(BulkHandler handler) {
		Assert.isNull(handler, "Nullable handler");
		bulkHandlers.remove(handler);
	}

	public int countOfHandlers() {
		return topicHandlers.size();
	}

	public int countOfHanders(String topic) {
		return topicHandlers.containsKey(topic) ? topicHandlers.get(topic).size() : 0;
	}

	public void startDaemon() {
		if (!isStarted()) {
			running.set(true);
			runner = ThreadUtils.runAsThread(this);
			log.info("TupleLoopProcessor is started.");
		}
	}

	public boolean isStarted() {
		return running.get();
	}

	public void stop() {
		running.set(false);
		if (runner != null) {
			try {
				runner.join();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			log.info("TupleLoopProcessor is stoped.");
		}
	}

	@Override
	public void onApplicationEvent(ContextRefreshedEvent event) {
		startDaemon();
	}

	@Override
	public void destroy() throws Exception {
		stop();
	}

	@Override
	public void run() {
		while (running.get()) {
			List<Tuple> tuples = null;
			try {
				tuples = bufferZone.get(collectionName, pullSize);
			} catch (Throwable e) {
				if (log.isTraceEnabled()) {
					log.trace(e.getMessage(), e);
				}
			}
			if (CollectionUtils.isNotEmpty(tuples)) {
				if (bulkHandlers.size() > 0) {
					Map<String, List<Tuple>> data = tuples.stream()
							.collect(Collectors.groupingBy(tuple -> tuple.getTopic(), Collectors.toList()));
					for (Map.Entry<String, List<Tuple>> entry : data.entrySet()) {
						String topic = entry.getKey();
						List<Tuple> bulk = entry.getValue();
						if (CollectionUtils.isNotEmpty(bulk)) {
							for (BulkHandler handler : bulkHandlers) {
								ExecutorUtils.runInBackground(threadPool, () -> {
									handler.onBatch(topic, bulk.stream().collect(Collectors.mapping(Tuple::copy, Collectors.toList())));
								});
							}
						}
					}
				}
				if (topicHandlers.size() > 0) {
					for (Tuple tuple : tuples) {
						List<Handler> handlers = topicHandlers.get(tuple.getTopic());
						if (CollectionUtils.isNotEmpty(handlers)) {
							for (Handler handler : handlers) {
								Tuple copy = tuple.copy();
								ExecutorUtils.runInBackground(threadPool, () -> {
									handler.onData(copy);
								});
							}
						}
					}
				}
				tuples = null;
			} else {
				ThreadUtils.randomSleep(1000L);
			}
		}
		log.info("Ending Loop!");
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		if (bean instanceof Handler) {
			addHandler((Handler) bean);
		} else if (bean instanceof BulkHandler) {
			addHandler((BulkHandler) bean);
		}
		return bean;
	}

}
