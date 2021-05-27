package indi.atlantis.framework.vortex.metric;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * 
 * SequenceRequest
 *
 * @author Fred Feng
 * @version 1.0
 */
@Getter
@Setter
@ToString
public class SequenceRequest {

	private String name;
	private String metric;
	private Object value;
	private long timestamp;

}
