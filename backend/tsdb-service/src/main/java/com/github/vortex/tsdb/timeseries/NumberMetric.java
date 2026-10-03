/*
 * Copyright 2017-2026 Fred Feng (paganini.fy@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.vortex.tsdb.timeseries;

/**
 * The aggregate of the numeric samples that fell into one time window.
 *
 * <p>An empty metric (count 0) answers null for every value rather than 0: "no data" and
 * "the data averages to 0" are different things, and a chart must be able to tell them apart.
 *
 * @Description: NumberMetric
 * @Author: Fred Feng
 * @Date: 15/11/2024
 * @Version 1.0.0
 */
public interface NumberMetric<T extends Number> extends UserMetric<NumberMetric<T>> {

    T getHighestValue();

    T getLowestValue();

    T getTotalValue();

    long getCount();

    Double getAverageValue();

    default boolean isEmpty() {
        return getCount() == 0;
    }
}
