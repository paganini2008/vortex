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
package io.atlantisframework.vortex.common;

import java.time.Instant;
import java.util.List;

import com.github.paganini2008.devtools.date.TimeSlot;

/**
 * 
 * Partitioner
 *
 * @author Fred Feng
 * @since 2.0.4
 */
public class TimeSlotPartitioner implements Partitioner {

	private final int span;
	private final TimeSlot timeSlot;

	public TimeSlotPartitioner(int span, TimeSlot timeSlot) {
		this.span = span;
		this.timeSlot = timeSlot;
	}

	@Override
	public <T> T selectChannel(Object data, List<T> channels) {
		Tuple tuple = (Tuple) data;
		long timestamp = tuple.getTimestamp();
		int hash = timeSlot.locate(Instant.ofEpochMilli(timestamp), span).hashCode();
		int index = (hash & 0x7FFFFFFF) % channels.size();
		return channels.get(index);
	}

}
