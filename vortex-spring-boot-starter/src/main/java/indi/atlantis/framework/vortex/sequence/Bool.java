package indi.atlantis.framework.vortex.sequence;

import java.util.HashMap;
import java.util.Map;

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

	public Map<String, Object> toEntries() {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("yes", yes);
		map.put("no", no);
		return map;
	}

}
