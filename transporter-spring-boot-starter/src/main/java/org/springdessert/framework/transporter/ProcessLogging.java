package org.springdessert.framework.transporter;

import java.util.Timer;
import java.util.concurrent.TimeUnit;

import org.springdessert.framework.transporter.buffer.BufferZone;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

import com.github.paganini2008.devtools.multithreads.Executable;
import com.github.paganini2008.devtools.multithreads.ThreadUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * 
 * ProcessLogging
 * 
 * @author Jimmy Hoff
 *
 * @since 1.0
 */
@Slf4j
public class ProcessLogging implements Executable, ApplicationListener<ContextRefreshedEvent>, DisposableBean {

	@Value("${spring.application.cluster.transport.bufferzone.collectionName}")
	private String collectionName;

	@Qualifier("producer")
	@Autowired
	private Counter producer;

	@Qualifier("consumer")
	@Autowired
	private Counter consumer;

	@Autowired
	private BufferZone bufferZone;

	private Timer timer;

	@Override
	public boolean execute() {
		if (log.isTraceEnabled()) {
			try {
				long remaining = bufferZone.size(collectionName);
				log.trace("[Process Producer] {}, remaining: {}", producer.toString(), remaining);
				log.trace("[Process Consumer] {}, remaining: {}", consumer.toString(), remaining);
			} catch (Exception ignored) {
			}
		}
		return true;
	}

	@Override
	public void onApplicationEvent(ContextRefreshedEvent event) {
		timer = ThreadUtils.scheduleAtFixedRate(this, 3, TimeUnit.SECONDS);
	}

	@Override
	public void destroy() throws Exception {
		if (timer != null) {
			timer.cancel();
		}
	}

}
