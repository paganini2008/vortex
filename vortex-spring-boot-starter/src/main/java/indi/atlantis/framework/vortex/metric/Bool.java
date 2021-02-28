package indi.atlantis.framework.vortex.metric;

import java.io.Serializable;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * 
 * Bool
 *
 * @author Jimmy Hoff
 * @version 1.0
 */
@Getter
@Setter
@ToString
public final class Bool implements Serializable{

	private static final long serialVersionUID = -3013190689650409680L;
	private long yes;
	private long no;

	public Bool(long yes, long no) {
		this.yes = yes;
		this.no = no;
	}

}
