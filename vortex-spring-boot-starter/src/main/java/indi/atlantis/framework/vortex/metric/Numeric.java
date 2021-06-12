/**
* Copyright 2021 Fred Feng (paganini.fy@gmail.com)

* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
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
 * @author Fred Feng
 *
 * @version 1.0
 */
@Getter
@Setter
@ToString
public final class Numeric implements Serializable, Cloneable {

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
	
	public Numeric clone() {
		try {
			return (Numeric) super.clone();
		} catch (CloneNotSupportedException e) {
			throw new IllegalStateException(e);
		}
	}

}
