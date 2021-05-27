package indi.atlantis.framework.vortex;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

/**
 * 
 * ServerInfo
 *
 * @author Fred Feng
 * @version 1.0
 */
public final class ServerInfo {

	private Map<String, Object> attributes = new HashMap<String, Object>();
	private String hostName;
	private int port;

	public ServerInfo() {
	}

	public ServerInfo(InetSocketAddress socketAddress) {
		this.hostName = socketAddress.getHostName();
		this.port = socketAddress.getPort();
	}

	public String getHostName() {
		return hostName;
	}

	public void setHostName(String hostName) {
		this.hostName = hostName;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public Map<String, Object> getAttributes() {
		return attributes;
	}

	public void setAttributes(Map<String, Object> attributes) {
		this.attributes = attributes;
	}

	public Object getAttribute(String name) {
		return attributes.get(name);
	}

	public void setAttribute(String name, Object attributeValue) {
		if (attributeValue != null) {
			attributes.put(name, attributeValue);
		} else {
			attributes.remove(name);
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (hostName != null ? 0 : hostName.hashCode());
		result = prime * result + Integer.hashCode(port);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof ServerInfo) {
			if (obj == this) {
				return true;
			}
			ServerInfo other = (ServerInfo) obj;
			return other.getHostName().equals(getHostName()) && other.getPort() == getPort();
		}
		return false;
	}

	@Override
	public String toString() {
		return hostName + ":" + port;
	}

}
