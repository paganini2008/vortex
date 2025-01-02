package com.github.vortex.tsd;

import java.math.BigDecimal;
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
 * @Description: DecimalTypeTsdStore
 * @Author: Fred Feng
 * @Date: 02/01/2025
 * @Version 1.0.0
 */
public class DecimalTypeTsdStore extends StringSimpleUserSamplerService<NumberMetric<BigDecimal>>
        implements TsdStore<NumberMetric<BigDecimal>> {

    public DecimalTypeTsdStore(int span, TimeWindowUnit timeWindowUnit, int maxSize,
            TssOverflowDataHandler<NumberMetric<BigDecimal>> dataManager) {
        super(span, timeWindowUnit, maxSize, Arrays.asList(dataManager));
    }

    @Override
    public String getDataType() {
        return "decimal";
    }

    @Override
    public void store(String category, String dimension, long timestamp,
            Consumer<UserSampler<NumberMetric<BigDecimal>>> consumer) {
        collect(category, dimension, timestamp, consumer);
    }

    @Override
    public UserSampler<NumberMetric<BigDecimal>> getEmptySampler(String category, String dimension,
            long timestamp) {
        return new UserSamplerImpl<>(timestamp, NumberMetrics.nullDecimalMetric(timestamp));
    }

    @Override
    public DataConverter<NumberMetric<BigDecimal>> getDataConverter() {
        return (value, packet) -> {
            BigDecimal acutalValue = ConvertUtils.convert(value, BigDecimal.class);
            return NumberMetrics.valueOf(acutalValue, packet.getTimestamp());
        };
    }

}
