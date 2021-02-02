package org.springdessert.framework.transporter.common;

/**
 * 
 * HttpClient
 * 
 * @author Jimmy Hoff
 *
 * @since 1.0
 */
public interface HttpClient extends LifeCycle, Client {

	void addHeader(String name, String value);

	default void setHeader(String name, String value) {
		addHeader(name, value);
	}

	default void send(Object data, Partitioner partitioner) {
		send(data);
	}

}
