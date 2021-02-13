package indi.atlantis.framework.vortex.sequence;

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
public final class Bool {

	private long yes;
	private long no;

	public Bool(long yes, long no) {
		this.yes = yes;
		this.no = no;
	}

}
