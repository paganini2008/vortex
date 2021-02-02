package org.springdessert.framework.transporter;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springdessert.framework.cluster.ApplicationInfo;
import org.springdessert.framework.cluster.multicast.ApplicationMessageListener;
import org.springdessert.framework.cluster.multicast.ApplicationMulticastEvent;
import org.springdessert.framework.cluster.multicast.ApplicationMulticastEvent.MulticastEventType;
import org.springdessert.framework.transporter.common.NioClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationListener;

import lombok.extern.slf4j.Slf4j;

/**
 * 
 * ApplicationTransportContext
 *
 * @author Jimmy Hoff
 * @version 1.0
 */
@Slf4j
public class ApplicationTransportContext implements ApplicationMessageListener, ApplicationListener<ApplicationMulticastEvent> {

	@Value("${spring.application.cluster.name}")
	private String clusterName;

	@Autowired
	private NioClient nioClient;

	private final Map<ApplicationInfo, ServerInfo> serverInfos = new ConcurrentHashMap<ApplicationInfo, ServerInfo>();

	public ServerInfo[] getServerInfos() {
		return serverInfos.values().toArray(new ServerInfo[0]);
	}

	@Override
	public void onApplicationEvent(ApplicationMulticastEvent event) {
		if (event.getMulticastEventType() == MulticastEventType.ON_INACTIVE) {
			serverInfos.remove(event.getApplicationInfo());
			log.info("Application '{}' has left transport cluster '{}'", event.getApplicationInfo(), clusterName);
		}
	}

	@Override
	public synchronized void onMessage(ApplicationInfo applicationInfo, String id, Object message) {
		ServerInfo serverInfo = (ServerInfo) message;
		nioClient.connect(new InetSocketAddress(serverInfo.getHostName(), serverInfo.getPort()), address -> {
			log.info("NioClient connect to {}", address);
			serverInfos.put(applicationInfo, serverInfo);
		});
	}

	@Override
	public String getTopic() {
		return ApplicationTransportContext.class.getName();
	}

}
