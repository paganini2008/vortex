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
package io.atlantisframework.vortex.streaming;

import java.time.Instant;
import java.util.List;

import com.github.paganini2008.devtools.collection.CollectionUtils;
import com.github.paganini2008.devtools.time.TimeWindowListener;

import lombok.extern.slf4j.Slf4j;

/**
 * 
 * SnapshotTimeWindowListener
 *
 * @author Fred Feng
 * @since 2.0.4
 */
@Slf4j
public class SnapshotTimeWindowListener<T> implements TimeWindowListener<T> {

	@Override
	public void saveCheckPoint(Instant time, List<T> values) {
		if (log.isInfoEnabled()) {
			if (CollectionUtils.isNotEmpty(values)) {
				log.info("[{}] size: {}, info: {}", time.toString(), values.size(), values.stream().findAny().get());
			}
		}
	}

}
