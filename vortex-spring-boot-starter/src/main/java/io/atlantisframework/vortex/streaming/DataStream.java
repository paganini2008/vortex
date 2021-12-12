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
package io.atlantisframework.vortex.streaming;

import com.github.paganini2008.devtools.time.TimeSlot;
import com.github.paganini2008.devtools.time.TimeWindowListener;
import com.github.paganini2008.devtools.time.TimeWindowMap;

import io.atlantisframework.vortex.Handler;
import io.atlantisframework.vortex.common.Partitioner;
import io.atlantisframework.vortex.common.TimeSlotPartitioner;
import io.atlantisframework.vortex.common.Tuple;

/**
 * 
 * DataStream
 *
 * @author Fred Feng
 * @since 2.0.4
 */
public class DataStream<T> implements Handler {

	public DataStream(String name, Class<T> outputType) {
		this.name = name;
		this.outputType = outputType;
	}

	private final String name;
	private final Class<T> outputType;
	private TimeWindowMap<T> timeWindowMap = new TimeWindowMap<>(1, TimeSlot.MINUTE, 100, new SnapshotTimeWindowListener<>());
	private TimeSlotPartitioner partitioner = new TimeSlotPartitioner(1, TimeSlot.MINUTE);

	public void timeWindow(int span, TimeSlot timeSlot, int batchSize, TimeWindowListener<T> timeWindowListener) {
		this.timeWindowMap = new TimeWindowMap<>(span, timeSlot, batchSize, timeWindowListener);
		this.partitioner = new TimeSlotPartitioner(span, timeSlot);
	}

	public void groupingBy(String... groupingFields) {
		partitioner.groupingBy(groupingFields);
	}

	@Override
	public void onData(Tuple tuple) {
		timeWindowMap.offer(tuple.getTimestamp(), tuple.toBean(outputType));
	}

	@Override
	public String getTopic() {
		return name;
	}

	@Override
	public Partitioner getPartitioner() {
		return partitioner;
	}

}
