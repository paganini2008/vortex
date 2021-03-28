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
public final class Bool implements Serializable, Cloneable {

	private static final long serialVersionUID = -3013190689650409680L;
	private long yes;
	private long no;

	public Bool(boolean yes) {
		this(yes, !yes);
	}

	public Bool(boolean yes, boolean no) {
		this(yes ? 1L : 0L, no ? 1L : 0L);
	}

	public Bool(long yes, long no) {
		this.yes = yes;
		this.no = no;
	}

	public Bool clone() {
		try {
			return (Bool) super.clone();
		} catch (CloneNotSupportedException e) {
			throw new IllegalStateException(e);
		}
	}

}
