package indi.atlantis.framework.vortex.common;

/**
 * 
 * TransportClientException
 * 
 * @author Fred Feng
 * 
 * 
 * @version 1.0
 */
public class TransportClientException extends RuntimeException {

	private static final long serialVersionUID = 232293217347009731L;

	public TransportClientException(String message) {
		super(message);
	}

	public TransportClientException(String message, Throwable cause) {
		super(message, cause);
	}

}
