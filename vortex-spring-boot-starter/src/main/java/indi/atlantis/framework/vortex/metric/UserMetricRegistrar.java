package indi.atlantis.framework.vortex.metric;

import indi.atlantis.framework.vortex.Handler;

/**
 * 
 * UserMetricRegistrar
 * 
 * @author Jimmy Hoff
 *
 * @version 1.0
 */
public class UserMetricRegistrar<V> implements MetricRegistrar {

	public UserMetricRegistrar(UserTypeHandler<String, V> typeHandler) {
		this.typeHandler = typeHandler;
	}

	private final UserTypeHandler<String, V> typeHandler;
	private final MetricSequencer<String, UserMetric<V>> primaryMetricSequencer = new SimpleMetricSequencer<String, UserMetric<V>>();
	private final MetricSequencer<String, UserMetric<V>> seconaryMetricSequencer = new SimpleMetricSequencer<String, UserMetric<V>>();

	@Override
	public void configure(int span, SpanUnit spanUnit, int bufferSize) {
		primaryMetricSequencer.setSpan(span).setSpanUnit(spanUnit).setBufferSize(bufferSize);
		seconaryMetricSequencer.setSpan(span).setSpanUnit(spanUnit).setBufferSize(bufferSize);
	}

	@Override
	public Sequencer getSequencer() {
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
