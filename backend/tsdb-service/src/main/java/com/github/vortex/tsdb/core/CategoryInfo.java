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

/**
 * A category and how busy it is.
 *
 * @param series           how many series it holds
 * @param lastSeen         when any of them last received a sample, accurate to the catalog
 *                         touch interval
 * @param samplesPerMinute the mean over the last {@link TsdStoreService#BUSY_WINDOW_BUCKETS}
 *                         complete buckets, all series together
 * 
 * @Description: CategoryInfo
 * @Author: Fred Feng
 * @Date: 03/10/2026
 * @Version 2.0.0
 */
public record CategoryInfo(String category, int series, long lastSeen, double samplesPerMinute) {
}
