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

	private final Sequencer<String, NumberMetric<Long>> longMetricSequencer = new Sequencer<String, NumberMetric<Long>>();
	private final Sequencer<String, NumberMetric<Double>> doubleMetricSequencer = new Sequencer<String, NumberMetric<Double>>();
	private final Sequencer<String, NumberMetric<BigDecimal>> decimalMetricSequencer = new Sequencer<String, NumberMetric<BigDecimal>>();
	private final Sequencer<String, UserMetric<Bool>> boolMetricSequencer = new Sequencer<String, UserMetric<Bool>>();

	public Sequencer<String, NumberMetric<Long>> longMetricSequencer() {
		return longMetricSequencer;
	}

	public Sequencer<String, NumberMetric<Double>> doubleMetricSequencer() {
		return doubleMetricSequencer;
	}

	public Sequencer<String, NumberMetric<BigDecimal>> decimalMetricSequencer() {
		return decimalMetricSequencer;
	}

	public Sequencer<String, UserMetric<Bool>> boolMetricSequencer() {
		return boolMetricSequencer;
	}

}
