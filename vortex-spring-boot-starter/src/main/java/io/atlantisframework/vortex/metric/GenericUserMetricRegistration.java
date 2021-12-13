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

import io.atlantisframework.vortex.Handler;
import io.atlantisframework.vortex.metric.api.MetricSequencer;
import io.atlantisframework.vortex.metric.api.SimpleMetricSequencer;
import io.atlantisframework.vortex.metric.api.UserMetric;
import io.atlantisframework.vortex.metric.api.UserMetricSequencer;

/**
 * 
 * GenericUserMetricRegistration
 * 
 * @author Fred Feng
 *
 * @since 2.0.1
 */
public class GenericUserMetricRegistration<V> implements UserMetricRegistrar<V> {

	public GenericUserMetricRegistration(UserMetricSequencer<String, V> metricSequencer, UserTypeHandler<V> typeHandler) {
		this.seconaryMetricSequencer = metricSequencer;
		this.primaryMetricSequencer = new SimpleMetricSequencer<>(metricSequencer.getSpan(), metricSequencer.getTimeWindowUnit(),
				metricSequencer.getBufferSize(), null);
		this.typeHandler = typeHandler;
	}

	private final UserTypeHandler<V> typeHandler;
	private final MetricSequencer<String, UserMetric<V>> primaryMetricSequencer;
	private final UserMetricSequencer<String, V> seconaryMetricSequencer;

	@Override
	public String getDataType() {
		return typeHandler.getDataTypeName();
	}

	@Override
	public UserMetricSequencer<String, V> getUserMetricSequencer() {
		return seconaryMetricSequencer;
	}

	@Override
	public Handler getHandler() {
		return new GenericUserMetricHandler<>(typeHandler.getDataTypeName(),
				new GenericUserMetricListener<>(primaryMetricSequencer, typeHandler));
	}

	@Override
	public Handler getSynchronizationHandler() {
		return new GenericUserMetricSynchronizationHandler<>(typeHandler.getDataTypeName() + "-", false,
				new GenericUserMetricListener<>(seconaryMetricSequencer, typeHandler));
	}

	@Override
	public Handler getIncrementalSynchronizationHandler() {
		return new GenericUserMetricSynchronizationHandler<>(typeHandler.getDataTypeName() + "+", true,
				new GenericUserMetricListener<>(seconaryMetricSequencer, typeHandler));
	}

	@Override
	public Synchronizer getSynchronizer() {
		return new GenericUserMetricSynchronizer<>(typeHandler.getDataTypeName() + "-", false,
				new GenericUserMetricListener<>(seconaryMetricSequencer, typeHandler));
	}

	@Override
	public Synchronizer getIncrementalSynchronizer() {
		return new GenericUserMetricSynchronizer<>(typeHandler.getDataTypeName() + "+", true,
				new GenericUserMetricListener<>(primaryMetricSequencer, typeHandler));
	}

}
