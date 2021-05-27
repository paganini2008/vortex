package indi.atlantis.framework.vortex.common.grizzly;

import org.glassfish.grizzly.Buffer;
import org.glassfish.grizzly.filterchain.AbstractCodecFilter;

import indi.atlantis.framework.vortex.common.Tuple;
import indi.atlantis.framework.vortex.common.serializer.KryoSerializer;
import indi.atlantis.framework.vortex.common.serializer.Serializer;

/**
 * 
 * TupleFilter
 *
 * @author Fred Feng
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
