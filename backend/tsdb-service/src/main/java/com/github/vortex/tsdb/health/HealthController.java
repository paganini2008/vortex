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
package com.github.vortex.tsdb.health;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.github.vortex.tsdb.web.ApiResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 *
 * @Description: HealthController
 * @Author: Fred Feng
 * @Date: 03/10/2026
 * @Version 2.0.0
 */
@Tag(name = "Cluster", description = "Cluster membership and health")
@RequestMapping("/tsd/health")
@RestController
public class HealthController {

    private final HealthService healthService;

    public HealthController(HealthService healthService) {
        this.healthService = healthService;
    }

    @Operation(summary = "Health of every instance",
            description = "Cache figures, replication channel and API traffic (requests per second, error "
                    + "rate) for every instance. Each instance writes its reading into the replicated cache "
                    + "every 5 s, so any instance answers from its own copy; one that has not reported for "
                    + "15 s is listed as unreachable.")
    @GetMapping
    public ApiResult<HealthService.ClusterHealth> cluster() {
        return ApiResult.ok(healthService.cluster());
    }

    @Operation(summary = "Health of the answering instance")
    @GetMapping("/self")
    public ApiResult<InstanceHealth> self() {
        return ApiResult.ok(healthService.self());
    }
}
