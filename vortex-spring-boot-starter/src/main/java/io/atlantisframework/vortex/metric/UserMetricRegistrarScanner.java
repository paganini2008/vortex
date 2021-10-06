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
package io.atlantisframework.vortex.metric;

import java.util.Map;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

import com.github.paganini2008.devtools.collection.MapUtils;

import io.atlantisframework.vortex.TupleLoopProcessor;
import lombok.extern.slf4j.Slf4j;

/**
 * 
 * UserMetricRegistrarScanner
 * 
 * @author Fred Feng
 *
 * @since 2.0.1
 */
@SuppressWarnings("all")
@Slf4j
public class UserMetricRegistrarScanner implements ApplicationListener<ContextRefreshedEvent> {

	@Override
	public void onApplicationEvent(ContextRefreshedEvent event) {
		ApplicationContext applicationContext = event.getApplicationContext();
		Map<String, UserMetricRegistrar> beans = applicationContext.getBeansOfType(UserMetricRegistrar.class);
		if (MapUtils.isNotEmpty(beans)) {
			UserSequencer sequencer = applicationContext.getBean(UserSequencer.class);
			TupleLoopProcessor tupleLoopProcessor = applicationContext.getBean(TupleLoopProcessor.class);
			SynchronizationExecutor incrementalSynchronizationExecutor = applicationContext.getBean("incrementalSynchronizationExecutor",
					SynchronizationExecutor.class);
			SynchronizationExecutor fullSynchronizationExecutor = applicationContext.getBean("fullSynchronizationExecutor",
					SynchronizationExecutor.class);
			String beanName;
			UserMetricRegistrar registrar;
			for (Map.Entry<String, UserMetricRegistrar> entry : beans.entrySet()) {
				beanName = entry.getKey();
				registrar = entry.getValue();
				tupleLoopProcessor.addHandler(registrar.getHandler());
				tupleLoopProcessor.addHandler(registrar.getIncrementalSynchronizationHandler());
				tupleLoopProcessor.addHandler(registrar.getSynchronizationHandler());
				incrementalSynchronizationExecutor.addSynchronizers(registrar.getIncrementalSynchronizer());
				fullSynchronizationExecutor.addSynchronizers(registrar.getSynchronizer());
				
				sequencer.registerDataType(registrar.getDataType(), registrar.getUserMetricSequencer());
			}

		}

	}

}
