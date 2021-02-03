package org.springtribe.framework.gearless.transport;

import java.net.InetSocketAddress;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationListener;
import org.springtribe.framework.cluster.ApplicationInfo;
import org.springtribe.framework.cluster.multicast.ApplicationMulticastEvent;
import org.springtribe.framework.cluster.multicast.ApplicationMulticastGroup;
import org.springtribe.framework.cluster.multicast.ApplicationMulticastEvent.MulticastEventType;
import org.springtribe.framework.cluster.utils.BeanLifeCycle;
import org.springtribe.framework.gearless.ApplicationTransportContext;
import org.springtribe.framework.gearless.ServerInfo;

import lombok.extern.slf4j.Slf4j;

/**
 * 
 * NioServerStarter
 *
 * @author Jimmy Hoff
 * @version 1.0
 */
@Slf4j
public class NioServerStarter implements BeanLifeCycle, ApplicationListener<ApplicationMulticastEvent> {

	public static final String DEFAULT_CHANNEL_PATTERN = "spring:application:cluster:%s:transport:starter";

	@Value("${spring.application.cluster.name}")
	private String clusterName;

	@Autowired
	private NioServer nioServer;

	@Autowired
	private ApplicationMulticastGroup applicationMulticastGroup;

	private InetSocketAddress socketAddress;

	@Override
	public void configure() throws Exception {
		socketAddress = (InetSocketAddress) nioServer.start();
	}

	@Override
	public void onApplicationEvent(ApplicationMulticastEvent event) {
		if (event.getMulticastEventType() == MulticastEventType.ON_ACTIVE) {
			ApplicationInfo applicationInfo = event.getApplicationInfo();
			applicationMulticastGroup.send(applicationInfo.getId(), ApplicationTransportContext.class.getName(),
					new ServerInfo(socketAddress));
			log.info("Application '{}' join transport cluster '{}'", applicationInfo, clusterName);
		}
	}

	@Override
	public void destroy() {
		nioServer.stop();
	}

}
