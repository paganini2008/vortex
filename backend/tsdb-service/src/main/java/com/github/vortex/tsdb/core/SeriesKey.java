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

import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

/**
 * Identifies one series: a data type, a category and a dimension.
 *
 * <p>Names are restricted to letters, digits, {@code _ . -}. That keeps {@code :} and
 * {@code |} free as separators, so a bucket key or a catalog member can be split back
 * unambiguously, and keeps {@code *} and {@code ?} out, so a series' key pattern can never
 * match another series.
 *
 * @Description: SeriesKey
 * @Author: Fred Feng
 * @Date: 03/10/2026
 * @Version 1.0.0
 */
public record SeriesKey(DataType dataType, String category, String dimension) {

    public static final String KEY_PREFIX = "tsd:";

    private static final Pattern NAME = Pattern.compile("^[\\p{L}\\p{N}_.\\-]{1,128}$");
    private static final char MEMBER_SEPARATOR = '|';

    public SeriesKey {
        if (dataType == null) {
            throw new IllegalArgumentException("Data type is required");
        }
        category = checkName("category", category);
        dimension = checkName("dimension", dimension);
    }

    public static SeriesKey of(String dataType, String category, String dimension) {
        return new SeriesKey(DataType.of(dataType), category, dimension);
    }

    /** Validates a category name on its own, for queries that cover a whole category. */
    public static String checkCategory(String category) {
        return checkName("category", category);
    }

    /** {@code tsd:<type>:<category>:<dimension>:<bucketStartMillis>} */
    public String bucketKey(long bucketStart) {
        return prefix() + bucketStart;
    }

    /** Matches every bucket of this series and nothing else. */
    public String keyPattern() {
        return prefix() + "*";
    }

    /**
     * The hash holding the latest sample of every series in a category, one field per series:
     * reading a whole category's instant values is then one local lookup.
     */
    public static String lastValueKey(String category) {
        return KEY_PREFIX + "last:" + category;
    }

    public String lastValueKey() {
        return lastValueKey(category);
    }

    /** {@code <type>|<dimension>}, the series' field in {@link #lastValueKey()}. */
    public String lastValueField() {
        return dataType.code() + MEMBER_SEPARATOR + dimension;
    }

    public static SeriesKey fromLastValueField(String category, String field) {
        int i = field.indexOf(MEMBER_SEPARATOR);
        if (i < 0) {
            throw new IllegalArgumentException("Malformed last value field");
        }
        return of(field.substring(0, i), category, field.substring(i + 1));
    }

    public byte[] catalogMember() {
        return (dataType.code() + MEMBER_SEPARATOR + category + MEMBER_SEPARATOR + dimension)
                .getBytes(StandardCharsets.UTF_8);
    }

    public static SeriesKey fromCatalogMember(byte[] member) {
        String[] parts = new String(member, StandardCharsets.UTF_8).split("\\|", -1);
        if (parts.length != 3) {
            throw new IllegalArgumentException("Malformed catalog member");
        }
        return of(parts[0], parts[1], parts[2]);
    }

    private String prefix() {
        return KEY_PREFIX + dataType.code() + ':' + category + ':' + dimension + ':';
    }

    private static String checkName(String what, String value) {
        if (value == null || !NAME.matcher(value.trim()).matches()) {
            throw new IllegalArgumentException("Invalid " + what + ": '" + value
                    + "' (1-128 letters, digits, '_', '.', '-')");
        }
        return value.trim();
    }
}
