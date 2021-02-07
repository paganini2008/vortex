package indi.atlantis.framework.gearless.common.grizzly;

import org.glassfish.grizzly.Buffer;
import org.glassfish.grizzly.filterchain.AbstractCodecFilter;

import indi.atlantis.framework.gearless.common.Tuple;
import indi.atlantis.framework.gearless.common.serializer.KryoSerializer;
import indi.atlantis.framework.gearless.common.serializer.Serializer;

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
