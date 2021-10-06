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

import java.util.Map;

/**
 * 
 * ResettableUserMetric
 * 
 * @author Fred Feng
 *
 * @since 2.0.1
 */
public class ResettableUserMetric<V> implements UserMetric<V> {

	private final UserMetric<V> real;

	ResettableUserMetric(UserMetric<V> real) {
		this.real = real;
	}

	@Override
	public long getTimestamp() {
		return real.getTimestamp();
	}

	@Override
	public boolean reset() {
		return Boolean.TRUE;
	}

	@Override
	public UserMetric<V> reset(UserMetric<V> currentMetric) {
		return real.reset(currentMetric);
	}

	@Override
	public UserMetric<V> merge(UserMetric<V> anotherMetric) {
		return real.merge(anotherMetric);
	}

	@Override
	public V get() {
		return real.get();
	}

	@Override
	public Map<String, Object> toEntries() {
		return real.toEntries();
	}

}
