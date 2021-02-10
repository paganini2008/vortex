package indi.atlantis.framework.vortex.sequence;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import indi.atlantis.framework.seafloor.ApplicationInfo;
import indi.atlantis.framework.seafloor.election.ApplicationClusterRefreshedEvent;
import indi.atlantis.framework.vortex.ApplicationTransportContext;
import indi.atlantis.framework.vortex.ServerInfo;
import indi.atlantis.framework.vortex.common.NioClient;
import lombok.extern.slf4j.Slf4j;

/**
 * 
 * IncrementalSynchronizationExecutor
 *
 * @author Jimmy Hoff
 * @version 1.0
 */
@Slf4j
public class IncrementalSynchronizationExecutor implements ApplicationListener<ApplicationClusterRefreshedEvent> {

	private static final int DEFAULT_SYNCHRONIZATION_PERIOD = 5;

	@Autowired
	private ApplicationTransportContext applicationTransportContext;

	@Autowired
	private ThreadPoolTaskScheduler taskScheduler;

	@Autowired
	private NioClient nioClient;

	private final List<Synchronizer> synchronizers = new ArrayList<Synchronizer>();

	private volatile ScheduledFuture<?> future;

	public void addSynchronizer(Synchronizer synchronizer) {
		if (synchronizer != null) {
			synchronizers.add(synchronizer);
		}
	}

	@Override
	public void onApplicationEvent(final ApplicationClusterRefreshedEvent event) {
		synchronizePeriodically(event.getLeaderInfo());
	}

	private void synchronizePeriodically(ApplicationInfo leaderInfo) {
		if (future != null) {
			future.cancel(false);
		}
		future = taskScheduler.scheduleWithFixedDelay(() -> {
			ServerInfo serverInfo = applicationTransportContext.getServerInfo(leaderInfo);
			if (serverInfo != null) {
				InetSocketAddress remoteAddress = new InetSocketAddress(serverInfo.getHostName(), serverInfo.getPort());
				for (Synchronizer synchronizer : synchronizers) {
					synchronizer.synchronize(nioClient, remoteAddress);
				}
			} else {
				log.warn("Leader nioserver is not available now.");
			}
		}, Duration.ofSeconds(DEFAULT_SYNCHRONIZATION_PERIOD));
		log.info("Start incremental synchronization to {} with {} seconds.", leaderInfo, DEFAULT_SYNCHRONIZATION_PERIOD);
	}

}
