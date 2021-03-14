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

	int getSpan();

	Sequencer setSpanUnit(SpanUnit spanUnit);

	default Sequencer setSpanUnit(int calendarField) {
		return setSpanUnit(SpanUnit.valueOf(calendarField));
	}

	SpanUnit getSpanUnit();

	Sequencer setBufferSize(int bufferSize);

	int getBufferSize();

	Map<String, Map<String, Object>> sequence(Object identifier, String metric, boolean asc, Function<Long, Map<String, Object>> render);

}