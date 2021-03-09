package indi.atlantis.framework.vortex.metric;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import indi.atlantis.framework.vortex.Handler;

/**
 * 
 * MetricSequencerAutoConfiguration
 *
 * @author Jimmy Hoff
 * @version 1.0
 */
@Import({ MetricSequencerController.class })
@Configuration
public class MetricSequencerAutoConfiguration {

	/**
	 * 
	 * BigIntMetricSequencerConfiguration
	 * 
	 * @author Jimmy Hoff
	 *
	 * @version 1.0
	 */
	@Configuration(proxyBeanMethods = false)
	public static class BigIntMetricSequencerConfiguration {

		@Bean
		public MetricRegistrar bigintMetricRegistrar() {
			BigIntTypeHandler typeHandler = new BigIntTypeHandler();
			return new UserMetricRegistrar<BigInt>(typeHandler) {

				@Override
				public Function<Long, Map<String, Object>> getRender() {
					return time -> {
						Map<String, Object> map = new HashMap<String, Object>();
						map.put("highestValue", 0L);
						map.put("middleValue", 0L);
						map.put("lowestValue", 0L);
						map.put("count", 0L);
						map.put("timestamp", time);
						return map;
					};
				}

			};
		}

		@Bean
		public Sequencer bigintSequencer(@Qualifier("bigintMetricRegistrar") MetricRegistrar bigintMetricRegistrar) {
			return bigintMetricRegistrar.getSequencer();
		}

		@Bean
		public Handler bigintMetricHandler(@Qualifier("bigintMetricRegistrar") MetricRegistrar bigintMetricRegistrar) {
			return bigintMetricRegistrar.getHandler();
		}

		@Bean
		public Handler bigintMetricSynchronizationHandler(@Qualifier("bigintMetricRegistrar") MetricRegistrar bigintMetricRegistrar) {
			return bigintMetricRegistrar.getSynchronizationHandler();
		}

		@Bean
		public Handler incrementalBigIntMetricSynchronizationHandler(
				@Qualifier("bigintMetricRegistrar") MetricRegistrar bigintMetricRegistrar) {
			return bigintMetricRegistrar.getIncrementalSynchronizationHandler();
		}

		@Bean
		public Synchronizer incrementalBigIntMetricSynchronizer(@Qualifier("bigintMetricRegistrar") MetricRegistrar bigintMetricRegistrar) {
			return bigintMetricRegistrar.getIncrementalSynchronizer();
		}

		@Bean
		public Synchronizer bigintMetricSynchronizer(@Qualifier("bigintMetricRegistrar") MetricRegistrar bigintMetricRegistrar) {
			return bigintMetricRegistrar.getSynchronizer();
		}
	}

	/**
	 * 
	 * NumericMetricSequencerConfiguration
	 * 
	 * @author Jimmy Hoff
	 *
	 * @version 1.0
	 */
	@Configuration(proxyBeanMethods = false)
	public static class NumericMetricSequencerConfiguration {

		@Bean
		public MetricRegistrar numericMetricRegistrar() {
			NumericTypeHandler typeHandler = new NumericTypeHandler();
			return new UserMetricRegistrar<Numeric>(typeHandler) {
				@Override
				public Function<Long, Map<String, Object>> getRender() {
					return time -> {
						Map<String, Object> map = new HashMap<String, Object>();
						map.put("highestValue", BigDecimal.ZERO);
						map.put("middleValue", BigDecimal.ZERO);
						map.put("lowestValue", BigDecimal.ZERO);
						map.put("count", 0L);
						map.put("timestamp", time);
						return map;
					};
				}
			};
		}

		@Bean
		public Sequencer numericSequencer(@Qualifier("numericMetricRegistrar") MetricRegistrar numericMetricRegistrar) {
			return numericMetricRegistrar.getSequencer();
		}

		@Bean
		public Handler numericMetricHandler(@Qualifier("numericMetricRegistrar") MetricRegistrar numericMetricRegistrar) {
			return numericMetricRegistrar.getHandler();
		}

		@Bean
		public Handler numericMetricSynchronizationHandler(@Qualifier("numericMetricRegistrar") MetricRegistrar numericMetricRegistrar) {
			return numericMetricRegistrar.getSynchronizationHandler();
		}

		@Bean
		public Handler incrementalNumericMetricSynchronizationHandler(
				@Qualifier("numericMetricRegistrar") MetricRegistrar numericMetricRegistrar) {
			return numericMetricRegistrar.getIncrementalSynchronizationHandler();
		}

		@Bean
		public Synchronizer incrementalNumericMetricSynchronizer(
				@Qualifier("numericMetricRegistrar") MetricRegistrar numericMetricRegistrar) {
			return numericMetricRegistrar.getIncrementalSynchronizer();
		}

		@Bean
		public Synchronizer numericMetricSynchronizer(@Qualifier("numericMetricRegistrar") MetricRegistrar numericMetricRegistrar) {
			return numericMetricRegistrar.getSynchronizer();
		}
	}

	/**
	 * 
	 * BoolMetricSequencerConfiguration
	 * 
	 * @author Jimmy Hoff
	 *
	 * @version 1.0
	 */
	@Configuration(proxyBeanMethods = false)
	public static class BoolMetricSequencerConfiguration {

		@Bean
		public MetricRegistrar boolMetricRegistrar() {
			BoolTypeHandler typeHandler = new BoolTypeHandler();
			return new UserMetricRegistrar<Bool>(typeHandler) {

				@Override
				public Function<Long, Map<String, Object>> getRender() {
					return time -> {
						Map<String, Object> map = new HashMap<String, Object>();
						map.put("yes", 0L);
						map.put("no", 0L);
						map.put("timestamp", time);
						return map;
					};
				}

			};
		}

		@Bean
		public Sequencer boolSequencer(@Qualifier("boolMetricRegistrar") MetricRegistrar boolMetricRegistrar) {
			return boolMetricRegistrar.getSequencer();
		}

		@Bean
		public Handler boolMetricHandler(@Qualifier("boolMetricRegistrar") MetricRegistrar boolMetricRegistrar) {
			return boolMetricRegistrar.getHandler();
		}

		@Bean
		public Handler boolMetricSynchronizationHandler(@Qualifier("boolMetricRegistrar") MetricRegistrar boolMetricRegistrar) {
			return boolMetricRegistrar.getSynchronizationHandler();
		}

		@Bean
		public Handler incrementalBoolMetricSynchronizationHandler(@Qualifier("boolMetricRegistrar") MetricRegistrar boolMetricRegistrar) {
			return boolMetricRegistrar.getIncrementalSynchronizationHandler();
		}

		@Bean
		public Synchronizer incrementalBoolMetricSynchronizer(@Qualifier("boolMetricRegistrar") MetricRegistrar boolMetricRegistrar) {
			return boolMetricRegistrar.getIncrementalSynchronizer();
		}

		@Bean
		public Synchronizer boolMetricSynchronizer(@Qualifier("boolMetricRegistrar") MetricRegistrar boolMetricRegistrar) {
			return boolMetricRegistrar.getSynchronizer();
		}

	}

	@Bean
	public IncrementalSynchronizationExecutor incrementalSynchronizationExecutor(
			@Qualifier("incrementalBoolMetricSynchronizer") Synchronizer boolMetricSynchronizer,
			@Qualifier("incrementalBigIntMetricSynchronizer") Synchronizer bigintMetricSynchronizer,
			@Qualifier("incrementalNumericMetricSynchronizer") Synchronizer numericMetricSynchronizer) {
		IncrementalSynchronizationExecutor synchronizationExecutor = new IncrementalSynchronizationExecutor();
		synchronizationExecutor.addSynchronizers(boolMetricSynchronizer, bigintMetricSynchronizer, numericMetricSynchronizer);
		return synchronizationExecutor;
	}

	@Bean
	public FullSynchronizationExecutor fullSynchronizationExecutor(@Qualifier("boolMetricSynchronizer") Synchronizer boolMetricSynchronizer,
			@Qualifier("bigintMetricSynchronizer") Synchronizer bigintMetricSynchronizer,
			@Qualifier("numericMetricSynchronizer") Synchronizer numericMetricSynchronizer) {
		FullSynchronizationExecutor synchronizationExecutor = new FullSynchronizationExecutor();
		synchronizationExecutor.addSynchronizers(boolMetricSynchronizer, bigintMetricSynchronizer, numericMetricSynchronizer);
		return synchronizationExecutor;
	}

}
