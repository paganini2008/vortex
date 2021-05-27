package indi.atlantis.framework.vortex.common;

/**
 * 
 * LifeCycle
 * 
 * @author Fred Feng
 * @version 1.0
 */
public interface LifeCycle {

	default void open() {
	}
	
	default void close() {
	}
	
	boolean isOpened();
	
}
