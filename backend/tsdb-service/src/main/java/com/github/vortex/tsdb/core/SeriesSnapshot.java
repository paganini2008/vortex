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
package com.github.vortex.tsdb.core;

import java.util.List;
import java.util.Map;

/**
 * One series over a range: what a dashboard panel shows.
 *
 * @param step    minutes between points
 * @param window  minutes each point aggregates: equal to {@code step} for tumbling windows,
 *                larger for sliding ones
 * @param last    the instant value, or null when the series has no recent sample
 * @param current the bucket now filling
 * @param summary the whole range folded into one
 * @param points  one aggregate per step, oldest first. {@code timestamp} is the start of the
 *                step, {@code from}/{@code to} the window it aggregates
 *
 * @Description: SeriesSnapshot
 * @Author: Fred Feng
 * @Date: 03/10/2026
 * @Version 2.0.0
 */
public record SeriesSnapshot(String dataType, String category, String dimension, String range,
        int step, int window, LastValue last, Map<String, Object> current, Map<String, Object> summary,
        List<Map<String, Object>> points) {
}
