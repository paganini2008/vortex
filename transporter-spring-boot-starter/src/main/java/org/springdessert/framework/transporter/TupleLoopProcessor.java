package org.springdessert.framework.transporter;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springdessert.framework.transporter.buffer.BufferZone;
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
import com.github.paganini2008.devtools.multithreads.ThreadUtils;
import org.springdessert.framework.transporter.common.Tuple;

import lombok.extern.slf4j.Slf4j;

/**
 * 
 * TupleLoopProcessor
 * 
 * @author Jimmy Hoff
 *
 * @since 1.0
 */
@Slf4j
public class TupleLoopProcessor implements Runnable, ApplicationListener<ContextRefreshedEvent>, BeanPostProcessor, DisposableBean {

	@Autowired
	private BufferZone bufferZone;

	@Qualifier("loopProcessorThreads")
	@Autowired(required = false)
	private ThreadPoolTaskExecutor threadPool;

	@Qualifier("consumer")
	@Autowired
	private Counter counter;

	@Value("${spring.application.cluster.transport.bufferzone.collectionName}")
	private String collectionName;

	@Value("${spring.application.cluster.transport.bufferzone.pullSize:1}")
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
		log.info("Add handler: {}", handler);
	}

	public void removeHandler(Handler handler) {
		Assert.isNull(handler, "Nullable handler");
		List<Handler> handlers = topicHandlers.get(handler.getTopic());
		if (handlers != null) {
			while (handlers.contains(handler)) {
				handlers.remove(handler);
			}
			log.info("Remove handler: {}", handler);
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
					for (BulkHandler handler : bulkHandlers) {
						handler.onBatch(tuples);
					}
				}
				if (topicHandlers.size() > 0) {
					for (Tuple tuple : tuples) {
						List<Handler> handlers = topicHandlers.get(tuple.getTopic());
						if (CollectionUtils.isNotEmpty(handlers)) {
							for (Handler handler : handlers) {
								Tuple copy = tuple.copy();
								if (threadPool != null) {
									threadPool.execute(() -> {
										handler.onData(copy);
									});
								} else {
									handler.onData(copy);
								}
							}
						}
					}
				}
				counter.incrementCount(tuples.size());
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
