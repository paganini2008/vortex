/**
* Copyright 2021 Fred Feng (paganini.fy@gmail.com)

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

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationListener;

import indi.atlantis.framework.tridenter.ApplicationInfo;
import indi.atlantis.framework.tridenter.multicast.ApplicationMessageListener;
import indi.atlantis.framework.tridenter.multicast.ApplicationMulticastEvent;
import indi.atlantis.framework.tridenter.multicast.ApplicationMulticastEvent.MulticastEventType;
import indi.atlantis.framework.vortex.common.NioClient;
import lombok.extern.slf4j.Slf4j;

/**
 * 
 * NioTransportContext
 *
 * @author Fred Feng
 * @version 1.0
 */
@Slf4j
public class NioTransportContext implements ApplicationMessageListener, ApplicationListener<ApplicationMulticastEvent> {

	@Value("${spring.application.cluster.name}")
	private String clusterName;

	@Autowired
	private NioClient nioClient;

	private final Map<ApplicationInfo, ServerInfo> serverInfos = new ConcurrentHashMap<ApplicationInfo, ServerInfo>();

	public ServerInfo[] getServerInfos() {
		return serverInfos.values().toArray(new ServerInfo[0]);
	}

	public ServerInfo[] getServerInfos(Predicate<ApplicationInfo> predicate) {
		List<ServerInfo> list = new ArrayList<ServerInfo>();
		for (Map.Entry<ApplicationInfo, ServerInfo> entry : serverInfos.entrySet()) {
			if (predicate.test(entry.getKey())) {
				list.add(entry.getValue());
			}
		}
		return list.toArray(new ServerInfo[0]);
	}

	public ServerInfo getServerInfo(ApplicationInfo applicationInfo) {
		return serverInfos.get(applicationInfo);
	}

	public int countOfServer() {
		return serverInfos.size();
	}

	@Override
	public synchronized void onApplicationEvent(ApplicationMulticastEvent event) {
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
		return NioTransportContext.class.getName();
	}

}
