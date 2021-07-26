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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.github.paganini2008.devtools.collection.MapUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * 
 * UserSequencer
 * 
 * @author Fred Feng
 *
 * @since 2.0.1
 */
@Slf4j
public final class UserSequencer {

	private final Map<String, UserMetricSequencer<String, ?>> registerMap = new ConcurrentHashMap<String, UserMetricSequencer<String, ?>>();

	public Map<String, Map<String, Object>> sequence(String dataType, String identifier, String metric, boolean asc) {
		return sequence(dataType, identifier, new String[] { metric }, asc);
	}

	public Map<String, Map<String, Object>> sequence(String dataType, String identifier, String[] metrics, boolean asc) {
		return registerMap.containsKey(dataType) ? registerMap.get(dataType).sequence(identifier, metrics, asc) : MapUtils.emptyMap();
	}

	public void registerDataType(String dataType, UserMetricSequencer<String, ?> metricSequencer) {
		registerMap.put(dataType, metricSequencer);
		log.info("Add metric sequencer '{}' with type '{}'.", metricSequencer.getClass().getName(), dataType);
	}

	public void removeDataType(String dataType) {
		if (registerMap.remove(dataType) != null) {
			log.info("Remove metric sequencer with type '{}'.", dataType);
		}

	}
}
