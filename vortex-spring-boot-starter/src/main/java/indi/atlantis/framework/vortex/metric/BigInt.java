/**
* Copyright 2017-2021 Fred Feng (paganini.fy@gmail.com)

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

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * 
 * BigInt
 * 
 * @author Fred Feng
 *
 * @version 1.0
 */
@Getter
@Setter
@ToString
public final class BigInt implements Serializable, Cloneable {

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

	public BigInt clone() {
		try {
			return (BigInt) super.clone();
		} catch (CloneNotSupportedException e) {
			throw new IllegalStateException(e);
		}
	}

}
