package indi.atlantis.framework.vortex.metric;

import java.io.Serializable;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * 
 * BigInt
 * 
 * @author Jimmy Hoff
 *
 * @version 1.0
 */
@Getter
@Setter
@ToString
public final class BigInt implements Serializable {

	private static final long serialVersionUID = 5844565831581821139L;

	private long highestValue;
	private long lowestValue;
	private long totalValue;
	private long count;

	public BigInt(long value) {
		this.highestValue = value;
		this.lowestValue = value;
		this.totalValue = value;
		this.count = 1;
	}

	public BigInt(long highestValue, long lowestValue, long totalValue, long count) {
		this.highestValue = highestValue;
		this.lowestValue = lowestValue;
		this.totalValue = totalValue;
		this.count = count;
	}

	public Long getMiddleValue() {
		return count > 0 ? totalValue / count : 0;
	}

}
