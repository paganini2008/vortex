package org.springtribe.framework.gearless.common;

/**
 * 
 * LifeCycle
 * 
 * @author Jimmy Hoff
 * @version 1.0
 */
public interface LifeCycle {

	default void open() {
	}
	
	default void close() {
	}
	
	boolean isOpened();
	
}
