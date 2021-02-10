package indi.atlantis.framework.vortex.sequence;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import indi.atlantis.framework.vortex.Handler;

/**
 * 
 * SequenceConfiguration
 *
 * @author Jimmy Hoff
 * @version 1.0
 */
@Configuration
public class SequenceConfiguration {

	@Bean
	public Environment primaryEnvironment() {
		return new Environment();
	}

	@Bean
	public Environment secondaryEnvironment() {
		return new Environment();
	}

	@Bean
	public Handler boolMetricHandler(@Qualifier("primaryEnvironment") Environment environment) {
		return new BoolMetricHandler("bool", environment.boolMetricSequencer());
	}

	@Bean
	public Handler boolMetricSynchronizationHandler(@Qualifier("secondaryEnvironment") Environment environment) {
		return new BoolMetricSynchronizationHandler("bool-", environment.boolMetricSequencer(), false);
	}

	@Bean
	public Handler incrementalBoolMetricSynchronizationHandler(@Qualifier("secondaryEnvironment") Environment environment) {
		return new BoolMetricSynchronizationHandler("bool+", environment.boolMetricSequencer(), true);
	}

	@Bean
	public Handler decimalMetricHandler(@Qualifier("primaryEnvironment") Environment environment) {
		return new DecimalMetricHandler("decimal", environment.decimalMetricSequencer());
	}

	@Bean
	public Handler decimalMetricSynchronizationHandler(@Qualifier("secondaryEnvironment") Environment environment) {
		return new DecimalMetricSynchronizationHandler("decimal-", environment.decimalMetricSequencer(), false);
	}

	@Bean
	public Handler incrementalDecimalMetricSynchronizationHandler(@Qualifier("secondaryEnvironment") Environment environment) {
		return new DecimalMetricSynchronizationHandler("decimal+", environment.decimalMetricSequencer(), true);
	}

	@Bean
	public Handler doubleMetricHandler(@Qualifier("primaryEnvironment") Environment environment) {
		return new DoubleMetricHandler("double", environment.doubleMetricSequencer());
	}

	@Bean
	public Handler longMetricHandler(@Qualifier("primaryEnvironment") Environment environment) {
		return new LongMetricHandler("long", environment.longMetricSequencer());
	}

	@Bean
	public Handler longMetricSynchronizationHandler(@Qualifier("secondaryEnvironment") Environment environment) {
		return new LongMetricSynchronizationHandler("long-", environment.longMetricSequencer(), false);
	}

	@Bean
	public Handler incrementalLongMetricSynchronizationHandler(@Qualifier("secondaryEnvironment") Environment environment) {
		return new LongMetricSynchronizationHandler("long+", environment.longMetricSequencer(), true);
	}

	@Bean
	public Synchronizer incrementalDecimalMetricSynchronizer(@Qualifier("primaryEnvironment") Environment environment) {
		return new DecimalMetricSynchronizer("decimal+", environment.decimalMetricSequencer(), true);
	}

	@Bean
	public Synchronizer decimalMetricSynchronizer(@Qualifier("secondaryEnvironment") Environment environment) {
		return new DecimalMetricSynchronizer("decimal-", environment.decimalMetricSequencer(), false);
	}

	@Bean
	public Synchronizer incrementalDoubleMetricSynchronizer(@Qualifier("primaryEnvironment") Environment environment) {
		return new DoubleMetricSynchronizer("double+", environment.doubleMetricSequencer(), true);
	}

	@Bean
	public Synchronizer doubleMetricSynchronizer(@Qualifier("secondaryEnvironment") Environment environment) {
		return new DoubleMetricSynchronizer("double-", environment.doubleMetricSequencer(), true);
	}

	@Bean
	public Synchronizer incrementalLongMetricSynchronizer(@Qualifier("primaryEnvironment") Environment environment) {
		return new LongMetricSynchronizer("long+", environment.longMetricSequencer(), true);
	}

	@Bean
	public Synchronizer longMetricSynchronizer(@Qualifier("secondaryEnvironment") Environment environment) {
		return new LongMetricSynchronizer("long-", environment.longMetricSequencer(), false);
	}

}
