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

import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;
import com.chaconneai.openspreader.cache.ProcessingCacheException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

/**
 * Fills {@code elapsed} and {@code requestPath} on every {@link ApiResult}, and turns errors
 * into one.
 *
 * @Description: ApiResultAdvice
 * @Author: Fred Feng
 * @Date: 03/10/2026
 * @Version 1.0.0
 */
@Slf4j
@RestControllerAdvice
public class ApiResultAdvice implements ResponseBodyAdvice<ApiResult<?>> {

    static final String REQUEST_START_ATTRIBUTE = ApiResultAdvice.class.getName() + ".start";

    @ExceptionHandler({IllegalArgumentException.class,
            MissingServletRequestParameterException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResult<Void> handleBadRequest(Exception e) {
        return ApiResult.failed(e.getMessage());
    }

    /**
     * A cache write fails only after retrying for the whole request timeout, which covers a
     * leader takeover. Past that the cluster really is unavailable, and the client should
     * retry later rather than treat its request as wrong.
     */
    @ExceptionHandler(ProcessingCacheException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public ApiResult<Void> handleCacheUnavailable(ProcessingCacheException e) {
        log.warn("Cluster cache unavailable: {}", e.getMessage());
        return ApiResult.failed("Cluster cache unavailable: " + e.getMessage());
    }

    @Override
    public boolean supports(MethodParameter returnType,
            Class<? extends HttpMessageConverter<?>> converterType) {
        return ApiResult.class.isAssignableFrom(returnType.getParameterType());
    }

    @Override
    public ApiResult<?> beforeBodyWrite(ApiResult<?> body, MethodParameter returnType,
            MediaType selectedContentType,
            Class<? extends HttpMessageConverter<?>> selectedConverterType,
            ServerHttpRequest request, ServerHttpResponse response) {
        if (body != null && request instanceof ServletServerHttpRequest servletRequest) {
            HttpServletRequest req = servletRequest.getServletRequest();
            body.setRequestPath(req.getRequestURI());
            if (req.getAttribute(REQUEST_START_ATTRIBUTE) instanceof Long start) {
                body.setElapsed(System.currentTimeMillis() - start);
            }
        }
        return body;
    }
}
