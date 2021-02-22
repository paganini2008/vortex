package indi.atlantis.framework.vortex.sequence;

import java.util.Map;

import lombok.Getter;
import lombok.Setter;

/**
 * 
 * Result
 * 
 * @author Jimmy Hoff
 *
 * @version 1.0
 */
@Getter
@Setter
public class Result {

	private final String dataType;
	private final String name;
	private final String metric;
	private Map<String, Map<String, Object>> data;

	public Result(String dataType, String name, String metric) {
		this.dataType = dataType;
		this.name = name;
		this.metric = metric;
	}

}
