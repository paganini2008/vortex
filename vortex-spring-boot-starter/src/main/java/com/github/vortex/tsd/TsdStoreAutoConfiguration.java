package com.github.vortex.tsd;

import java.math.BigDecimal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import com.github.doodler.common.timeseries.NumberMetric;
import com.github.doodler.common.utils.TimeWindowUnit;
import com.github.vortex.tss.TssOverflowDataHandler;
import com.github.vortex.tss.TssRedisOverflowDataManager;

/**
 * 
 * @Description: TssConfig
 * @Author: Fred Feng
 * @Date: 02/01/2025
 * @Version 1.0.0
 */
@EnableConfigurationProperties({TsdStoreProperties.class})
@ComponentScan("com.github.vortex.tsd")
@Configuration(proxyBeanMethods = false)
public class TsdStoreAutoConfiguration {

    @Autowired
    private TsdStoreProperties tssProperties;

    @Bean("decimalTypeTssOverflowDataManager")
    public TssOverflowDataHandler<NumberMetric<BigDecimal>> decimalTypeTssRedisOverflowDataManager(
            RedisTemplate<String, Object> redisTemplate) {
        return new TssRedisOverflowDataManager<>("tss:decimal", redisTemplate);
    }

    @Bean
    public DecimalTypeTsdStore decimalTypeTsdStore(
            @Qualifier("decimalTypeTssOverflowDataManager") TssOverflowDataHandler<NumberMetric<BigDecimal>> dataManager) {
        return new DecimalTypeTsdStore(tssProperties.getSpan(), TimeWindowUnit.MINUTES,
                tssProperties.getOverflowSize(), dataManager);
    }

    @Bean("longTypeTssOverflowDataManager")
    public TssOverflowDataHandler<NumberMetric<Long>> longTypeTssRedisOverflowDataManager(
            RedisTemplate<String, Object> redisTemplate) {
        return new TssRedisOverflowDataManager<>("tss:long", redisTemplate);
    }

    @Bean
    public LongTypeTsdStore longTypeTsdStore(
            @Qualifier("longTypeTssOverflowDataManager") TssOverflowDataHandler<NumberMetric<Long>> dataManager) {
        return new LongTypeTsdStore(tssProperties.getSpan(), TimeWindowUnit.MINUTES,
                tssProperties.getOverflowSize(), dataManager);
    }

    @Bean("doubleTypeTssOverflowDataManager")
    public TssOverflowDataHandler<NumberMetric<Double>> doubleTypeTssRedisOverflowDataManager(
            RedisTemplate<String, Object> redisTemplate) {
        return new TssRedisOverflowDataManager<>("tss:double", redisTemplate);
    }

    @Bean
    public DoubleTypeTsdStore doubleTypeTsdStore(
            @Qualifier("doubleTypeTssOverflowDataManager") TssOverflowDataHandler<NumberMetric<Double>> dataManager) {
        return new DoubleTypeTsdStore(tssProperties.getSpan(), TimeWindowUnit.MINUTES,
                tssProperties.getOverflowSize(), dataManager);
    }

}
