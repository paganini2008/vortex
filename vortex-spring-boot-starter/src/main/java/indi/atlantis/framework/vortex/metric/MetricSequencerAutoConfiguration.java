package indi.atlantis.framework.vortex.metric;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * 
 * MetricSequencerAutoConfiguration
 *
 * @author Jimmy Hoff
 * @version 1.0
 */
@Import({ SequencerController.class })
@Configuration
public class MetricSequencerAutoConfiguration {

	@Bean
	public Sequencer sequencer() {
		return new Sequencer();
	}

	@Bean
	public UserMetricRegistrar<BigInt> bigIntMetricRegistrar() {
		return new GenericUserMetricRegistration<BigInt>(new BigIntMetricSequencer(new LoggingMetricEvictionHandler<>()),
				new BigIntTypeHandler());
	}

	@Bean
	public UserMetricRegistrar<Numeric> numericMetricRegistrar() {
		return new GenericUserMetricRegistration<Numeric>(new NumericMetricSequencer(new LoggingMetricEvictionHandler<>()),
				new NumericTypeHandler());
	}

	@Bean
	public UserMetricRegistrar<Bool> boolMetricRegistrar() {
		return new GenericUserMetricRegistration<Bool>(new BoolMetricSequencer(new LoggingMetricEvictionHandler<>()),
				new BoolTypeHandler());
	}

	@Bean
	public SynchronizationExecutor incrementalSynchronizationExecutor() {
		return new IncrementalSynchronizationExecutor();
	}

	@Bean
	public SynchronizationExecutor fullSynchronizationExecutor() {
		return new FullSynchronizationExecutor();
	}

}
