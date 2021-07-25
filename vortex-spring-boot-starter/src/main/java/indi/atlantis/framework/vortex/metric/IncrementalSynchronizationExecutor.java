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
package indi.atlantis.framework.vortex.metric;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledFuture;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import indi.atlantis.framework.tridenter.ApplicationInfo;
import indi.atlantis.framework.tridenter.election.ApplicationClusterRefreshedEvent;
import indi.atlantis.framework.vortex.NioTransportContext;
import indi.atlantis.framework.vortex.ServerInfo;
import indi.atlantis.framework.vortex.common.NioClient;
import lombok.extern.slf4j.Slf4j;

/**
 * 
 * IncrementalSynchronizationExecutor
 *
 * @author Fred Feng
 * @version 1.0
 */
@Slf4j
public class IncrementalSynchronizationExecutor
		implements ApplicationListener<ApplicationClusterRefreshedEvent>, SynchronizationExecutor, DisposableBean {

	public static final int DEFAULT_SYNCHRONIZATION_PERIOD = 5;

	@Autowired
	private NioTransportContext transportContext;

	@Autowired
	private ThreadPoolTaskScheduler taskScheduler;

	@Autowired
	private NioClient nioClient;

	private final List<Synchronizer> synchronizers = new CopyOnWriteArrayList<Synchronizer>();

	private volatile ScheduledFuture<?> future;

	public void addSynchronizers(Synchronizer... synchronizers) {
		if (synchronizers != null) {
			this.synchronizers.addAll(Arrays.asList(synchronizers));
		}
	}

	public void clearSynchronizers() {
		this.synchronizers.clear();
	}

	@Override
	public void onApplicationEvent(final ApplicationClusterRefreshedEvent event) {
		synchronizePeriodically(event.getLeaderInfo());
	}

	@Override
	public void synchronizePeriodically(ApplicationInfo leaderInfo) {
		if (future != null) {
			future.cancel(false);
		}
		future = taskScheduler.scheduleWithFixedDelay(() -> {
			ServerInfo serverInfo = transportContext.getServerInfo(leaderInfo);
			if (serverInfo != null) {
				InetSocketAddress remoteAddress = new InetSocketAddress(serverInfo.getHostName(), serverInfo.getPort());
				for (Synchronizer synchronizer : synchronizers) {
					synchronizer.synchronize(nioClient, remoteAddress);
				}
			} else {
				log.trace("Leader nioserver is not available now.");
			}
		}, Duration.ofSeconds(DEFAULT_SYNCHRONIZATION_PERIOD));
		log.info("Start incremental synchronization to {} with {} seconds.", leaderInfo, DEFAULT_SYNCHRONIZATION_PERIOD);
	}

	@Override
	public void destroy() throws Exception {
		if (future != null) {
			future.cancel(true);
		}
	}

}
