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
 * @Description: LongTypeTsdStore
 * @Author: Fred Feng
 * @Date: 02/01/2025
 * @Version 1.0.0
 */
public class LongTypeTsdStore extends StringSimpleUserSamplerService<NumberMetric<Long>>
        implements TsdStore<NumberMetric<Long>> {

    public LongTypeTsdStore(int span, TimeWindowUnit timeWindowUnit, int maxSize,
            TssOverflowDataHandler<NumberMetric<Long>> dataManager) {
        super(span, timeWindowUnit, maxSize, Arrays.asList(dataManager));
    }

    @Override
    public String getDataType() {
        return "long";
    }

    @Override
    public void store(String category, String dimension, long timestamp,
            Consumer<UserSampler<NumberMetric<Long>>> consumer) {
        collect(category, dimension, timestamp, consumer);
    }

    @Override
    public UserSampler<NumberMetric<Long>> getEmptySampler(String category, String dimension,
            long timestamp) {
        return new UserSamplerImpl<>(timestamp, NumberMetrics.nullLongMetric(timestamp));
    }

    @Override
    public DataConverter<NumberMetric<Long>> getDataConverter() {
        return (value, packet) -> {
            Long acutalValue = ConvertUtils.convert(value, Long.class);
            return NumberMetrics.valueOf(acutalValue, packet.getTimestamp());
        };
    }

}
