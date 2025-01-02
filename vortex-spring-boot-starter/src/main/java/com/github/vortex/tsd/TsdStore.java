package com.github.vortex.tsd;

import java.util.function.Consumer;
import com.github.doodler.common.timeseries.UserMetric;
import com.github.doodler.common.timeseries.UserSampler;

/**
 * 
 * @Description: TsdStore
 * @Author: Fred Feng
 * @Date: 02/01/2025
 * @Version 1.0.0
 */
public interface TsdStore<T extends UserMetric<T>> {

    void store(String category, String dimension, long timestamp,
            Consumer<UserSampler<T>> consumer);

    String getDataType();

    DataConverter<T> getDataConverter();

}
