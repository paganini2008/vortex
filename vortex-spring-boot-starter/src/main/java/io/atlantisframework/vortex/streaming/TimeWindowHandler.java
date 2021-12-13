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

import org.springframework.beans.factory.annotation.Autowired;

import io.atlantisframework.vortex.Handler;
import io.atlantisframework.vortex.common.MultipleChoicePartitioner;
import io.atlantisframework.vortex.common.NioClient;
import io.atlantisframework.vortex.common.Partitioner;
import io.atlantisframework.vortex.common.Tuple;

/**
 * 
 * TimeWindowHandler
 *
 * @author Fred Feng
 * @since 2.0.4
 */
public class TimeWindowHandler implements Handler {

	private static final String KEYWORD = "name";

	@Autowired
	private NioClient nioClient;

	@Autowired
	private Partitioner partitioner;

	@Override
	public void onData(Tuple tuple) {
		if (partitioner instanceof MultipleChoicePartitioner) {
			String dataStreamName = tuple.getField(KEYWORD, String.class);
			tuple.setField("topic", dataStreamName);
			nioClient.send(tuple, partitioner);
		}
	}

	@Override
	public String getTopic() {
		return "timeWindow";
	}

}
