package com.github.vortex.tsd;

import org.springframework.boot.context.properties.ConfigurationProperties;
import lombok.Data;

/**
 * 
 * @Description: TsdStoreProperties
 * @Author: Fred Feng
 * @Date: 02/01/2025
 * @Version 1.0.0
 */
@ConfigurationProperties("vortex.tsd")
@Data
public class TsdStoreProperties {

    private int span = 1;
    private int overflowSize = 1;
    private int displaySize = 60;

}
