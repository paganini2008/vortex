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
package io.atlantisframework.vortex.metric;

import java.io.Serializable;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * 
 * Bool
 *
 * @author Fred Feng
 * @since 2.0.1
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
