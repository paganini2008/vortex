package indi.atlantis.framework.vortex.metric;

import java.util.Map;
import java.util.function.Function;

/**
 * 
 * Sequencer
 * 
 * @author Jimmy Hoff
 *
 * @version 1.0
 */
public interface Sequencer {

	Sequencer setSpan(int span);

	Sequencer setSpanUnit(SpanUnit spanUnit);

	default Sequencer setSpanUnit(int calendarField) {
		return setSpanUnit(SpanUnit.valueOf(calendarField));
	}

	Sequencer setBufferSize(int bufferSize);

	Map<String, Map<String, Object>> sequence(String identifier, String metric, boolean asc, Function<Long, Map<String, Object>> render);

}