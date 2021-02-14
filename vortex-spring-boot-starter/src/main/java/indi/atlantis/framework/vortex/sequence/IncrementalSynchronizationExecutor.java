package indi.atlantis.framework.vortex.sequence;

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

import indi.atlantis.framework.seafloor.ApplicationInfo;
import indi.atlantis.framework.seafloor.election.ApplicationClusterRefreshedEvent;
import indi.atlantis.framework.vortex.NioTransportContext;
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
public class IncrementalSynchronizationExecutor implements ApplicationListener<ApplicationClusterRefreshedEvent>, DisposableBean {

	public static final int DEFAULT_SYNCHRONIZATION_PERIOD = 5;

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
	public void onApplicationEvent(final ApplicationClusterRefreshedEvent event) {
		synchronizePeriodically(event.getLeaderInfo());
	}

	private void synchronizePeriodically(ApplicationInfo leaderInfo) {
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
				log.warn("Leader nioserver is not available now.");
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
