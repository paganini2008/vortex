package indi.atlantis.framework.vortex.metric;

import java.util.Map;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

import com.github.paganini2008.devtools.collection.MapUtils;

import indi.atlantis.framework.vortex.TupleLoopProcessor;
import lombok.extern.slf4j.Slf4j;

/**
 * 
 * UserMetricRegistrarScanner
 * 
 * @author Jimmy Hoff
 *
 * @version 1.0
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
