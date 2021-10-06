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
package io.atlantisframework.vortex.utils;

import org.nustaq.serialization.FSTConfiguration;

import io.atlantisframework.vortex.common.Tuple;

/**
 * 
 * FstKafkaSerializer
 * 
 * @author Fred Feng
 *
 * @since 2.0.1
 */
public class FstKafkaSerializer implements KafkaSerializer {

	private final FSTConfiguration configuration = FSTConfiguration.createDefaultConfiguration();

	@Override
	public byte[] serialize(String topic, Tuple data) {
		return configuration.asByteArray(data);
	}

	@Override
	public Tuple deserialize(String topic, byte[] data) {
		return (Tuple) configuration.asObject(data);
	}

}
