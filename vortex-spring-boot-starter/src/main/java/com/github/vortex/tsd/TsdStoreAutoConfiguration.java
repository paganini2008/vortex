package com.github.vortex.tsd;

import static com.github.doodler.common.Constants.ISO8601_DATE_TIME_PATTERN;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.github.doodler.common.timeseries.NumberMetric;
import com.github.doodler.common.utils.JacksonUtils;
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

    @Bean("decimalTypeOverflowDataManager")
    public TssOverflowDataHandler<NumberMetric<BigDecimal>> decimalTypeTssRedisOverflowDataManager(
            RedisConnectionFactory connectionFactory) {
        return new TssRedisOverflowDataManager<>("tss:decimal",
                new Jackson2JsonRedisTemplate(connectionFactory));
    }

    @Bean
    public DecimalTypeTsdStore decimalTypeTsdStore(
            @Qualifier("decimalTypeOverflowDataManager") TssOverflowDataHandler<NumberMetric<BigDecimal>> dataManager) {
        return new DecimalTypeTsdStore(tssProperties.getSpan(), TimeWindowUnit.MINUTES,
                tssProperties.getOverflowSize(), dataManager);
    }

    @Bean("longTypeOverflowDataManager")
    public TssOverflowDataHandler<NumberMetric<Long>> longTypeTssRedisOverflowDataManager(
            RedisConnectionFactory connectionFactory) {
        return new TssRedisOverflowDataManager<>("tss:long",
                new Jackson2JsonRedisTemplate(connectionFactory));
    }

    @Bean
    public LongTypeTsdStore longTypeTsdStore(
            @Qualifier("longTypeOverflowDataManager") TssOverflowDataHandler<NumberMetric<Long>> dataManager) {
        return new LongTypeTsdStore(tssProperties.getSpan(), TimeWindowUnit.MINUTES,
                tssProperties.getOverflowSize(), dataManager);
    }

    @Bean("doubleTypeOverflowDataManager")
    public TssOverflowDataHandler<NumberMetric<Double>> doubleTypeTssRedisOverflowDataManager(
            RedisConnectionFactory connectionFactory) {
        return new TssRedisOverflowDataManager<>("tss:double",
                new Jackson2JsonRedisTemplate(connectionFactory));
    }

    @Bean
    public DoubleTypeTsdStore doubleTypeTsdStore(
            @Qualifier("doubleTypeOverflowDataManager") TssOverflowDataHandler<NumberMetric<Double>> dataManager) {
        return new DoubleTypeTsdStore(tssProperties.getSpan(), TimeWindowUnit.MINUTES,
                tssProperties.getOverflowSize(), dataManager);
    }

    public static class Jackson2JsonRedisTemplate extends RedisTemplate<String, Object> {

        public Jackson2JsonRedisTemplate(RedisConnectionFactory redisConnectionFactory) {
            this(redisConnectionFactory, getJacksonRedisSerializer());
        }

        public Jackson2JsonRedisTemplate(RedisConnectionFactory redisConnectionFactory,
                Jackson2JsonRedisSerializer<Object> jackson2JsonRedisSerializer) {
            super();
            setConnectionFactory(redisConnectionFactory);
            setKeySerializer(RedisSerializer.string());
            setValueSerializer(jackson2JsonRedisSerializer);
            setHashKeySerializer(RedisSerializer.string());
            setHashValueSerializer(jackson2JsonRedisSerializer);
            afterPropertiesSet();
        }
    }

    public static Jackson2JsonRedisSerializer<Object> getJacksonRedisSerializer() {
        ObjectMapper om = new ObjectMapper();
        om.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        om.activateDefaultTyping(LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL);
        om.setDateFormat(new SimpleDateFormat(ISO8601_DATE_TIME_PATTERN));
        SimpleModule javaTimeModule = JacksonUtils.getJavaTimeModuleForWebMvc();
        om.registerModule(javaTimeModule);
        Jackson2JsonRedisSerializer<Object> jackson2JsonRedisSerializer =
                new Jackson2JsonRedisSerializer<Object>(Object.class);
        jackson2JsonRedisSerializer.setObjectMapper(om);
        return jackson2JsonRedisSerializer;
    }

}
