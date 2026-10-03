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

import java.nio.ByteBuffer;

/**
 * The latest sample of a series: its instant value.
 *
 * <p>"Latest" is the last one the cluster leader applied. Two samples pushed to different nodes
 * within a few milliseconds of each other may land in either order.
 *
 * @param value     presented in the series' type: a long series reports an integer
 * @param timestamp when the sample arrived, epoch milliseconds
 *
 * @Description: LastValue
 * @Author: Fred Feng
 * @Date: 03/10/2026
 * @Version 2.0.0
 */
public record LastValue(Number value, long timestamp) {

    static byte[] encode(double value, long timestamp) {
        return ByteBuffer.allocate(16).putDouble(value).putLong(timestamp).array();
    }

    static LastValue decode(DataType dataType, byte[] bytes) {
        if (bytes == null || bytes.length != 16) {
            return null;
        }
        ByteBuffer buf = ByteBuffer.wrap(bytes);
        double value = buf.getDouble();
        return new LastValue(dataType.present(value), buf.getLong());
    }
}
