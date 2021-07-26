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
 * @since 2.0.1
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
