package indi.atlantis.framework.vortex.aggregation;

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
public class Bool {

	private long yes;
	private long no;

	public Bool(long yes, long no) {
		this.yes = yes;
		this.no = no;
	}

}
