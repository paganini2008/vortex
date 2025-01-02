package com.github.vortex.tsd;

import static com.github.doodler.common.timeseries.TimeSeriesConstants.DEFAULT_DATE_TIME_FORMATTER;
import java.time.Instant;
import java.util.Calendar;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import com.github.doodler.common.timeseries.OverflowDataManager;
import com.github.doodler.common.utils.TimeWindowUnit;

/**
 * 
 * @Description: TsdStoreService
 * @Author: Fred Feng
 * @Date: 02/01/2025
 * @Version 1.0.0
 */
@Component
public class TsdStoreService {

    @Qualifier("decimalTypeTssOverflowDataManager")
    @Autowired
    private OverflowDataManager decimalTypeOverflowDataManager;

    @Qualifier("longTypeTssOverflowDataManager")
    @Autowired
    private OverflowDataManager longTypeOverflowDataManager;

    @Qualifier("doubleTypeTssOverflowDataManager")
    @Autowired
    private OverflowDataManager doubleTypeOverflowDataManager;

    @Autowired
    private DecimalTypeTsdStore decimalTypeTsdStore;

    @Autowired
    private LongTypeTsdStore longTypeTsdStore;

    @Autowired
    private DoubleTypeTsdStore doubleTypeTsdStore;

    @Autowired
    private TsdStoreProperties tssProperties;

    public Map<String, Object> retrieveWithDecimalType(String category, String dimension,
            TimeZone timeZone) {
        Map<Instant, Object> all = new TreeMap<>(Comparator.reverseOrder());
        Map<Instant, Object> data = decimalTypeTsdStore.sequence(category, dimension);
        all.putAll(data);
        data = decimalTypeOverflowDataManager.retrieve(category, dimension,
                tssProperties.getDisplaySize());
        all.putAll(data);
        Map<String, Object> results =
                all.entrySet().stream().collect(LinkedHashMap::new,
                        (m, e) -> m.put(e.getKey().atZone(timeZone.toZoneId())
                                .format(DEFAULT_DATE_TIME_FORMATTER), e.getValue()),
                        LinkedHashMap::putAll);
        Calendar c = Calendar.getInstance(timeZone);
        Map<String, Object> emptyMap = TimeWindowUnit.MINUTES.initializeMap(c.getTime(),
                tssProperties.getSpan(), tssProperties.getDisplaySize(), timeZone,
                DEFAULT_DATE_TIME_FORMATTER, time -> decimalTypeTsdStore
                        .getEmptySampler(category, dimension, time).getSample().represent());
        return mergeMaps(emptyMap, results);
    }

    public Map<String, Object> retrieveWithLongType(String category, String dimension,
            TimeZone timeZone) {
        Map<Instant, Object> all = new TreeMap<>(Comparator.reverseOrder());
        Map<Instant, Object> data = longTypeTsdStore.sequence(category, dimension);
        all.putAll(data);
        data = longTypeOverflowDataManager.retrieve(category, dimension,
                tssProperties.getDisplaySize());
        all.putAll(data);
        Map<String, Object> results =
                all.entrySet().stream().collect(LinkedHashMap::new,
                        (m, e) -> m.put(e.getKey().atZone(timeZone.toZoneId())
                                .format(DEFAULT_DATE_TIME_FORMATTER), e.getValue()),
                        LinkedHashMap::putAll);
        Calendar c = Calendar.getInstance(timeZone);
        Map<String, Object> emptyMap = TimeWindowUnit.MINUTES.initializeMap(c.getTime(),
                tssProperties.getSpan(), tssProperties.getDisplaySize(), timeZone,
                DEFAULT_DATE_TIME_FORMATTER, time -> longTypeTsdStore
                        .getEmptySampler(category, dimension, time).getSample().represent());
        return mergeMaps(emptyMap, results);
    }

    public Map<String, Object> retrieveWithDoubleType(String category, String dimension,
            TimeZone timeZone) {
        Map<Instant, Object> all = new TreeMap<>(Comparator.reverseOrder());
        Map<Instant, Object> data = doubleTypeTsdStore.sequence(category, dimension);
        all.putAll(data);
        data = doubleTypeOverflowDataManager.retrieve(category, dimension,
                tssProperties.getDisplaySize());
        all.putAll(data);
        Map<String, Object> results =
                all.entrySet().stream().collect(LinkedHashMap::new,
                        (m, e) -> m.put(e.getKey().atZone(timeZone.toZoneId())
                                .format(DEFAULT_DATE_TIME_FORMATTER), e.getValue()),
                        LinkedHashMap::putAll);
        Calendar c = Calendar.getInstance(timeZone);
        Map<String, Object> emptyMap = TimeWindowUnit.MINUTES.initializeMap(c.getTime(),
                tssProperties.getSpan(), tssProperties.getDisplaySize(), timeZone,
                DEFAULT_DATE_TIME_FORMATTER, time -> doubleTypeTsdStore
                        .getEmptySampler(category, dimension, time).getSample().represent());
        return mergeMaps(emptyMap, results);
    }

    private Map<String, Object> mergeMaps(Map<String, Object> emptyMap,
            Map<String, Object> results) {
        Set<String> times = new HashSet<>(results.keySet());
        for (String time : times) {
            if (!emptyMap.containsKey(time)) {
                results.remove(time);
            }
        }
        emptyMap.putAll(results);
        return emptyMap;
    }

}
