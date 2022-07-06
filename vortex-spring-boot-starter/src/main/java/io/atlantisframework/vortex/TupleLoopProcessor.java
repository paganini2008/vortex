/**
* Copyright 2017-2022 Fred Feng (paganini.fy@gmail.com)

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
package io.atlantisframework.vortex;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import com.github.paganini2008.devtools.Assert;
import com.github.paganini2008.devtools.collection.CollectionUtils;
import com.github.paganini2008.devtools.collection.MapUtils;
import com.github.paganini2008.devtools.multithreads.ExecutorUtils;
import com.github.paganini2008.devtools.multithreads.ThreadUtils;

import io.atlantisframework.vortex.buffer.BufferZone;
import io.atlantisframework.vortex.common.MultipleChoicePartitioner;
import io.atlantisframework.vortex.common.Partitioner;
import io.atlantisframework.vortex.common.Tuple;
import lombok.extern.slf4j.Slf4j;

/**
 * 
 * TupleLoopProcessor
 * 
 * @author Fred Feng
 *
 * @since 2.0.1
 */
@Slf4j
public class TupleLoopProcessor implements Runnable, ApplicationListener<ContextRefreshedEvent>, DisposableBean {

	@Autowired
	private BufferZone bufferZone;

	@Qualifier("loopProcessorThreadPool")
	@Autowired(required = false)
	private ThreadPoolTaskExecutor threadPool;

	@Autowired
	private Partitioner partitioner;

	@Value("${atlantis.framework.vortex.bufferzone.collectionName}")
	private String collectionName;

	@Value("${atlantis.framework.vortex.bufferzone.pullSize:1}")
	private int pullSize;

	private final Map<String, List<Handler>> topicHandlers = new ConcurrentHashMap<String, List<Handler>>();
	private final AtomicBoolean running = new AtomicBoolean(false);
	private Thread runner;

	public void addHandler(Handler handler) {
		Assert.isNull(handler, "NonNull handler");
		List<Handler> handlers = MapUtils.get(topicHandlers, handler.getTopic(), () -> new CopyOnWriteArrayList<Handler>());
		handlers.add(handler);
		if (handler.getPartitioner() != null && partitioner instanceof MultipleChoicePartitioner) {
			((MultipleChoicePartitioner) partitioner).addPartitioner(handler.getTopic(), handler.getPartitioner());
		}
		log.info("Add handler: {}/{}", handler.getTopic(), handler);
	}

	public void removeHandler(Handler handler) {
		Assert.isNull(handler, "NonNull handler");
		List<Handler> handlers = topicHandlers.get(handler.getTopic());
		if (handlers != null) {
			while (handlers.contains(handler)) {
				handlers.remove(handler);
			}
			log.info("Remove handler: {}/{}", handler.getTopic(), handler);
		}
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
		event.getApplicationContext().getBeansOfType(Handler.class).entrySet().forEach(e -> {
			addHandler(e.getValue());
		});
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

}
