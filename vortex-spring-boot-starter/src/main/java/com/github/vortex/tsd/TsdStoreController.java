package com.github.vortex.tsd;

import java.time.ZoneId;
import java.util.Collections;
import java.util.Map;
import java.util.TimeZone;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.github.doodler.common.ApiResult;
import com.github.doodler.common.transmitter.NioClient;
import com.github.doodler.common.transmitter.Packet;

/**
 * 
 * @Description: TsdStoreController
 * @Author: Fred Feng
 * @Date: 02/01/2025
 * @Version 1.0.0
 */
@RequestMapping("/tsd")
@RestController
public class TsdStoreController {

    @Autowired
    private TsdStoreService tssService;

    @Autowired
    private NioClient nioClient;

    @PostMapping("/push")
    public ApiResult<Packet> push(@RequestParam("t") String dataType,
            @RequestParam("c") String category, @RequestParam("d") String dimension,
            @RequestParam("v") String value) {
        Packet packet = Packet
                .wrap(Map.of("dataType", dataType, "category", category, "dimension", dimension));
        packet.setObject(value);
        nioClient.send(packet);
        return ApiResult.ok(packet);
    }

    @GetMapping("/retrieve")
    public ApiResult<Map<String, Object>> retrieve(@RequestParam("t") String dataType,
            @RequestParam("c") String category, @RequestParam("d") String dimension,
            @RequestParam(name = "z", required = false) String zone) {
        Map<String, Object> results = Collections.emptyMap();
        TimeZone timeZone = StringUtils.isNotBlank(zone) ? TimeZone.getTimeZone(ZoneId.of(zone))
                : TimeZone.getTimeZone("Australia/Sydney");
        switch (dataType) {
            case "decimal":
                results = tssService.retrieveWithDecimalType(category, dimension, timeZone);
                break;
            case "long":
                results = tssService.retrieveWithLongType(category, dimension, timeZone);
                break;
            case "double":
                results = tssService.retrieveWithDoubleType(category, dimension, timeZone);
                break;
            default:
                throw new IllegalArgumentException("Unexpected value: " + dataType);
        }
        return ApiResult.ok(results);
    }

}
