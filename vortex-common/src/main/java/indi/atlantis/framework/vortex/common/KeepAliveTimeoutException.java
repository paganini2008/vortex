package indi.atlantis.framework.vortex.common;

/**
 * 
 * KeepAliveTimeoutException
 *
 * @author Fred Feng
 * @version 1.0
 */
public class KeepAliveTimeoutException extends RuntimeException {

	private static final long serialVersionUID = -3214862285809923018L;

	public KeepAliveTimeoutException() {
		super();
	}

	public KeepAliveTimeoutException(String msg) {
		super(msg);
	}

}
