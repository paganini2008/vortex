/**
* Copyright 2017-2022 Fred Feng (paganini.fy@gmail.com)

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
package io.atlantisframework.vortex.metric.api;

import java.util.HashMap;
import java.util.Map;

/**
 * 
 * BoolMetric
 *
 * @author Fred Feng
 * @since 2.0.1
 */
public class BoolMetric extends AbstractUserMetric<Bool> {

	public BoolMetric(boolean yes, long timestamp) {
		this(new Bool(yes), timestamp);
	}

	public BoolMetric(boolean yes, boolean no, long timestamp) {
		this(new Bool(yes, no), timestamp);
	}

	public BoolMetric(Bool bool, long timestamp) {
		super(bool, timestamp, false);
	}

	@Override
	public UserMetric<Bool> reset(UserMetric<Bool> newMetric) {
		Bool current = get();
		Bool update = newMetric.get();
		long yes = current.getYes() - update.getYes();
		long no = current.getNo() - update.getNo();
		Bool bool = new Bool(yes, no);
		return new BoolMetric(bool, newMetric.getTimestamp());
	}

	@Override
	public UserMetric<Bool> merge(UserMetric<Bool> newMetric) {
		Bool current = get();
		Bool update = newMetric.get();
		long yes = current.getYes() + update.getYes();
		long no = current.getNo() + update.getNo();
		Bool bool = new Bool(yes, no);
		return new BoolMetric(bool, newMetric.getTimestamp());
	}

	public Map<String, Object> toEntries() {
		Bool bool = get();
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("yes", bool.getYes());
		map.put("no", bool.getNo());
		map.put("timestamp", getTimestamp());
		return map;
	}

}
