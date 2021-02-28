package indi.atlantis.framework.vortex.metrics;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import indi.atlantis.framework.vortex.metric.Environment;

/**
 * 
 * EnvironmentConfig
 * 
 * @author Jimmy Hoff
 *
 * @version 1.0
 */
@Configuration
public class EnvironmentConfig {

	@Value("${atlantis.framework.vortex.environment.span:1}")
	private int span;

	@Value("${atlantis.framework.vortex.environment.bufferSize:120}")
	private int bufferSize;

	@Value("${atlantis.framework.vortex.environment.spanUnit:12}")
	private int spanUnitValue;

	@Autowired
	public void configureEnvironment(@Qualifier("primaryEnvironment") Environment primaryEnvironment,
			@Qualifier("secondaryEnvironment") Environment secondaryEnvironment) {
		primaryEnvironment.boolMetricSequencer().setSpan(span).setBufferSize(bufferSize).setSpanUnit(spanUnitValue);
		primaryEnvironment.longMetricSequencer().setSpan(span).setBufferSize(bufferSize).setSpanUnit(spanUnitValue);
		primaryEnvironment.doubleMetricSequencer().setSpan(span).setBufferSize(bufferSize).setSpanUnit(spanUnitValue);
		primaryEnvironment.decimalMetricSequencer().setSpan(span).setBufferSize(bufferSize).setSpanUnit(spanUnitValue);

		secondaryEnvironment.boolMetricSequencer().setSpan(span).setBufferSize(bufferSize).setSpanUnit(spanUnitValue);
		secondaryEnvironment.longMetricSequencer().setSpan(span).setBufferSize(bufferSize).setSpanUnit(spanUnitValue);
		secondaryEnvironment.doubleMetricSequencer().setSpan(span).setBufferSize(bufferSize).setSpanUnit(spanUnitValue);
		secondaryEnvironment.decimalMetricSequencer().setSpan(span).setBufferSize(bufferSize).setSpanUnit(spanUnitValue);
	}

}
