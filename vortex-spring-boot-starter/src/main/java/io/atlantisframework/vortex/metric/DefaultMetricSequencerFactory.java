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
package io.atlantisframework.vortex.metric;

import io.atlantisframework.vortex.metric.api.BigInt;
import io.atlantisframework.vortex.metric.api.BigIntMetricSequencer;
import io.atlantisframework.vortex.metric.api.Bool;
import io.atlantisframework.vortex.metric.api.BoolMetricSequencer;
import io.atlantisframework.vortex.metric.api.GenericUserMetricSequencer;
import io.atlantisframework.vortex.metric.api.Numeric;
import io.atlantisframework.vortex.metric.api.NumericMetricSequencer;

/**
 * DefaultMetricSequencerFactory
 * 
 * @author Fred Feng
 *
 * @since 2.0.1
 */
public class DefaultMetricSequencerFactory implements MetricSequencerFactory {
	
	public GenericUserMetricSequencer<String, BigInt> getBigIntMetricSequencer() {
		return new BigIntMetricSequencer(new LoggingMetricEvictionHandler<>());
	}

	public GenericUserMetricSequencer<String, Numeric> getNumericMetricSequencer() {
		return new NumericMetricSequencer(new LoggingMetricEvictionHandler<>());
	}

	public GenericUserMetricSequencer<String, Bool> getBoolMetricSequencer() {
		return new BoolMetricSequencer(new LoggingMetricEvictionHandler<>());
	}
	
}
