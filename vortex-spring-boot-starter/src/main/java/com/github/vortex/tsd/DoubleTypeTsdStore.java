package com.github.vortex.tsd;

import java.util.Arrays;
import java.util.function.Consumer;
import com.github.doodler.common.timeseries.NumberMetric;
import com.github.doodler.common.timeseries.NumberMetrics;
import com.github.doodler.common.timeseries.StringSimpleUserSamplerService;
import com.github.doodler.common.timeseries.UserSampler;
import com.github.doodler.common.timeseries.UserSamplerImpl;
import com.github.doodler.common.utils.ConvertUtils;
import com.github.doodler.common.utils.TimeWindowUnit;
import com.github.vortex.tss.TssOverflowDataHandler;

/**
 * 
 * @Description: DoubleTypeTsdStore
 * @Author: Fred Feng
 * @Date: 02/01/2025
 * @Version 1.0.0
 */
public class DoubleTypeTsdStore extends StringSimpleUserSamplerService<NumberMetric<Double>>
        implements TsdStore<NumberMetric<Double>> {

    public DoubleTypeTsdStore(int span, TimeWindowUnit timeWindowUnit, int maxSize,
            TssOverflowDataHandler<NumberMetric<Double>> dataManager) {
        super(span, timeWindowUnit, maxSize, Arrays.asList(dataManager));
    }

    @Override
    public String getDataType() {
        return "double";
    }

    @Override
    public void store(String category, String dimension, long timestamp,
            Consumer<UserSampler<NumberMetric<Double>>> consumer) {
        collect(category, dimension, timestamp, consumer);
    }

    @Override
    public UserSampler<NumberMetric<Double>> getEmptySampler(String category, String dimension,
            long timestamp) {
        return new UserSamplerImpl<>(timestamp, NumberMetrics.nullDoubleMetric(timestamp));
    }

    @Override
    public DataConverter<NumberMetric<Double>> getDataConverter() {
        return (value, packet) -> {
            Double acutalValue = ConvertUtils.convert(value, Double.class);
            return NumberMetrics.valueOf(acutalValue, packet.getTimestamp());
        };
    }
}
