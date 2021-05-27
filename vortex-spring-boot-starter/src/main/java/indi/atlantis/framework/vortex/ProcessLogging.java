package indi.atlantis.framework.vortex;

import java.util.Timer;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

import com.github.paganini2008.devtools.multithreads.Executable;
import com.github.paganini2008.devtools.multithreads.ThreadUtils;

import indi.atlantis.framework.vortex.buffer.BufferZone;
import lombok.extern.slf4j.Slf4j;

/**
 * 
 * ProcessLogging
 * 
 * @author Fred Feng
 *
 * @since 1.0
 */
@Slf4j
public class ProcessLogging implements Executable, ApplicationListener<ContextRefreshedEvent>, DisposableBean {

	@Value("${atlantis.framework.vortex.bufferzone.collectionName}")
	private String collectionName;

	@Autowired
	private Accumulator accumulator;

	@Autowired
	private BufferZone bufferZone;

	private Timer timer;

	@Override
	public boolean execute() {
		if (log.isTraceEnabled()) {
			try {
				long remainingSize = bufferZone.size(collectionName);
				log.trace(accumulator.toString() + ", remainingSize: " + remainingSize);
			} catch (Exception ignored) {
			}
		}
		return true;
	}

	@Override
	public void onApplicationEvent(ContextRefreshedEvent event) {
		timer = ThreadUtils.scheduleWithFixedDelay(this, 3, TimeUnit.SECONDS);
	}

	@Override
	public void destroy() throws Exception {
		if (timer != null) {
			timer.cancel();
		}
	}

}
