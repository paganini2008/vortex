package com.github.vortex.tsd;

import com.github.doodler.common.timeseries.UserMetric;
import com.github.doodler.common.transmitter.Packet;

/**
 * 
 * @Description: DataConverter
 * @Author: Fred Feng
 * @Date: 02/01/2025
 * @Version 1.0.0
 */
@FunctionalInterface
public interface DataConverter<T extends UserMetric<T>> {

    T convert(Object value, Packet packet);

}
