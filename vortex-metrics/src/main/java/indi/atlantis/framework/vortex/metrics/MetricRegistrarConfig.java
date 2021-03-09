package indi.atlantis.framework.vortex.metrics;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import indi.atlantis.framework.vortex.metric.MetricRegistrar;
import indi.atlantis.framework.vortex.metric.SpanUnit;

/**
 * 
 * MetricRegistrarConfig
 * 
 * @author Jimmy Hoff
 *
 * @version 1.0
 */
@Configuration
public class MetricRegistrarConfig {

	@Value("${atlantis.framework.vortex.environment.span:1}")
	private int span;

	@Value("${atlantis.framework.vortex.environment.bufferSize:120}")
	private int bufferSize;

	@Value("${atlantis.framework.vortex.environment.spanUnit:12}")
	private int calendarField;

	@Autowired
	public void configureDefaultMetricRegistrars(@Qualifier("bigintMetricRegistrar") MetricRegistrar bigintMetricRegistrar,
			@Qualifier("numericMetricRegistrar") MetricRegistrar numericMetricRegistrar,
			@Qualifier("boolMetricRegistrar") MetricRegistrar boolMetricRegistrar) {
		bigintMetricRegistrar.configure(span, SpanUnit.valueOf(calendarField), bufferSize);
		numericMetricRegistrar.configure(span, SpanUnit.valueOf(calendarField), bufferSize);
		boolMetricRegistrar.configure(span, SpanUnit.valueOf(calendarField), bufferSize);
	}

}
