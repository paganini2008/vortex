/**
* Copyright 2018-2021 Fred Feng (paganini.fy@gmail.com)

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

import com.github.paganini2008.devtools.collection.AtomicReferenceMap;

/**
 * 
 * MetricCollectorMap
 *
 * @author Fred Feng
 * @version 1.0
 */
public class MetricCollectorMap<T extends Metric<T>> extends AtomicReferenceMap<String, T> {

	private static final long serialVersionUID = 6743810257952172449L;

	public MetricCollectorMap(boolean ordered) {
		super(ordered);
	}

	@Override
	protected T merge(String key, T current, T update) {
		if (current != null) {
			return update.reset() ? current.reset(update) : current.merge(update);
		}
		return update;
	}

}
