package com.github.vortex.tss;

import com.github.doodler.common.timeseries.OverflowDataHandler;
import com.github.doodler.common.timeseries.UserMetric;

/**
 * 
 * @Description: TssOverflowDataHandler
 * @Author: Fred Feng
 * @Date: 02/01/2025
 * @Version 1.0.0
 */
public interface TssOverflowDataHandler<T extends UserMetric<T>>
        extends OverflowDataHandler<String, String, T> {

}
