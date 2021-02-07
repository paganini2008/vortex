package indi.atlantis.framework.gearless.common;

/**
 * 
 * Client
 * 
 * @author Jimmy Hoff
 *
 * @since 1.0
 */
public interface Client {

	void send(Object data);

	void send(Object data, Partitioner partitioner);

}
