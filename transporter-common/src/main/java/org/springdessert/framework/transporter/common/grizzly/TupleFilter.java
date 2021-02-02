package org.springdessert.framework.transporter.common.grizzly;

import org.glassfish.grizzly.Buffer;
import org.glassfish.grizzly.filterchain.AbstractCodecFilter;
import org.springdessert.framework.transporter.common.Tuple;
import org.springdessert.framework.transporter.common.serializer.KryoSerializer;
import org.springdessert.framework.transporter.common.serializer.Serializer;

/**
 * 
 * TupleFilter
 *
 * @author Jimmy Hoff
 * @version 1.0
 */
public class TupleFilter extends AbstractCodecFilter<Buffer, Tuple> {

	public TupleFilter() {
		this(new KryoSerializer());
	}

	public TupleFilter(Serializer serializer) {
		this(new GrizzlyTupleCodecFactory(serializer));
	}

	public TupleFilter(TupleCodecFactory codecFactory) {
		super(codecFactory.getDecoder(), codecFactory.getEncoder());
	}

}
