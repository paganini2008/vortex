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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ScheduledFuture;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import indi.atlantis.framework.tridenter.ApplicationInfo;
import indi.atlantis.framework.tridenter.InstanceId;
import indi.atlantis.framework.tridenter.LeaderState;
import indi.atlantis.framework.tridenter.election.ApplicationClusterLeaderEvent;
import indi.atlantis.framework.vortex.NioTransportContext;
import indi.atlantis.framework.vortex.ServerInfo;
import indi.atlantis.framework.vortex.common.NioClient;
import lombok.extern.slf4j.Slf4j;

/**
 * 
 * FullSynchronizationExecutor
 *
 * @author Fred Feng
 * @since 2.0.1
 */
@Slf4j
public class FullSynchronizationExecutor
		implements ApplicationListener<ApplicationClusterLeaderEvent>, SynchronizationExecutor, DisposableBean {

	public static final int DEFAULT_SYNCHRONIZATION_PERIOD = 5;

	@Autowired
	private InstanceId instanceId;

	@Autowired
	private NioTransportContext transportContext;

	@Autowired
	private ThreadPoolTaskScheduler taskScheduler;

	@Autowired
	private NioClient nioClient;

	private final List<Synchronizer> synchronizers = new ArrayList<Synchronizer>();

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
	public void onApplicationEvent(ApplicationClusterLeaderEvent event) {
		if (event.getLeaderState() == LeaderState.UP) {
			ApplicationInfo leaderInfo = instanceId.getApplicationInfo();
			synchronizePeriodically(leaderInfo);
		}
	}

	@Override
	public void synchronizePeriodically(ApplicationInfo leaderInfo) {
		if (future != null) {
			throw new IllegalStateException("Full synchronization is running now.");
		}
		future = taskScheduler.scheduleWithFixedDelay(() -> {
			ServerInfo[] serverInfos = transportContext.getServerInfos(info -> {
				return !info.equals(leaderInfo);
			});
			InetSocketAddress remoteAddress;
			for (ServerInfo serverInfo : serverInfos) {
				remoteAddress = new InetSocketAddress(serverInfo.getHostName(), serverInfo.getPort());
				for (Synchronizer synchronizer : synchronizers) {
					synchronizer.synchronize(nioClient, remoteAddress);
				}
			}
		}, Duration.ofSeconds(DEFAULT_SYNCHRONIZATION_PERIOD));
		log.info("Start full synchronization from {} with {} seconds.", instanceId.getApplicationInfo(), DEFAULT_SYNCHRONIZATION_PERIOD);
	}

	@Override
	public void destroy() throws Exception {
		if (future != null) {
			future.cancel(true);
		}
	}

}
