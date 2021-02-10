package indi.atlantis.framework.vortex.sequence;

import java.math.BigDecimal;

/**
 * 
 * Environment
 *
 * @author Jimmy Hoff
 * @version 1.0
 */
public class Environment {

	private final MetricSequencer<String, NumberMetric<Long>> longMetricSequencer = new MetricSequencer<String, NumberMetric<Long>>();
	private final MetricSequencer<String, NumberMetric<Double>> doubleMetricSequencer = new MetricSequencer<String, NumberMetric<Double>>();
	private final MetricSequencer<String, NumberMetric<BigDecimal>> decimalMetricSequencer = new MetricSequencer<String, NumberMetric<BigDecimal>>();
	private final MetricSequencer<String, UserMetric<Bool>> boolMetricSequencer = new MetricSequencer<String, UserMetric<Bool>>();

	public MetricSequencer<String, NumberMetric<Long>> longMetricSequencer() {
		return longMetricSequencer;
	}

	public MetricSequencer<String, NumberMetric<Double>> doubleMetricSequencer() {
		return doubleMetricSequencer;
	}

	public MetricSequencer<String, NumberMetric<BigDecimal>> decimalMetricSequencer() {
		return decimalMetricSequencer;
	}

	public MetricSequencer<String, UserMetric<Bool>> boolMetricSequencer() {
		return boolMetricSequencer;
	}

}
