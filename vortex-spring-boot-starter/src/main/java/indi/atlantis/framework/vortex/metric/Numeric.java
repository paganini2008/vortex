package indi.atlantis.framework.vortex.metric;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * 
 * Numeric
 * 
 * @author Jimmy Hoff
 *
 * @version 1.0
 */
@Getter
@Setter
@ToString
public final class Numeric implements Serializable {

	private static final long serialVersionUID = -6456889194956954035L;

	private BigDecimal highestValue;
	private BigDecimal lowestValue;
	private BigDecimal totalValue;
	private long count;

	public Numeric(BigDecimal value) {
		this.highestValue = value;
		this.lowestValue = value;
		this.totalValue = value;
		this.count = 1;
	}

	public Numeric(BigDecimal highestValue, BigDecimal lowestValue, BigDecimal totalValue, long count) {
		this.highestValue = highestValue;
		this.lowestValue = lowestValue;
		this.totalValue = totalValue;
		this.count = count;
	}

	public BigDecimal getMiddleValue() {
		return count > 0 ? totalValue.divide(BigDecimal.valueOf(count), 4, RoundingMode.HALF_UP) : BigDecimal.ZERO;
	}

}
