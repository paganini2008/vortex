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
package com.github.vortex.tsdb.web;

import lombok.Getter;
import lombok.Setter;

/**
 * The response envelope of the previous version (doodler's {@code ApiResult}), field for
 * field, so existing clients keep parsing it.
 *
 * @Description: ApiResult
 * @Author: Fred Feng
 * @Date: 02/01/2025
 * @Version 2.0.0
 */
@Getter
@Setter
public class ApiResult<T> {

    public static final int SUCCESS = 1;
    public static final int FAILURE = 0;

    /** 1: success, 0: failure */
    private int code;
    private String msg;
    private T data;
    /** Milliseconds spent handling the request */
    private long elapsed;
    private String requestPath;

    public static <T> ApiResult<T> ok(T data) {
        ApiResult<T> rs = new ApiResult<>();
        rs.setCode(SUCCESS);
        rs.setMsg("ok");
        rs.setData(data);
        return rs;
    }

    public static <T> ApiResult<T> failed(String msg) {
        ApiResult<T> rs = new ApiResult<>();
        rs.setCode(FAILURE);
        rs.setMsg(msg);
        return rs;
    }
}
