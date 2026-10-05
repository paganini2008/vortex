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

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.chaconneai.openspreader.cache.ProcessingCache;
import com.chaconneai.openspreader.cluster.WebAddressMetadataListener;
import com.chaconneai.spreader.GossipCluster;
import com.chaconneai.spreader.Node;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Which nodes make up the cluster and which one leads, as seen from the node answering.
 *
 * @Description: ClusterController
 * @Author: Fred Feng
 * @Date: 03/10/2026
 * @Version 1.0.0
 */
@Tag(name = "Cluster", description = "Cluster membership and health")
@RequestMapping("/tsd")
@RestController
public class ClusterController {

    private final GossipCluster cluster;
    private final ProcessingCache cache;

    public ClusterController(GossipCluster cluster, ProcessingCache cache) {
        this.cluster = cluster;
        this.cache = cache;
    }

    @Operation(summary = "Show the cluster",
            description = "The nodes of the cluster and the current leader, as seen by the answering node.")
    @GetMapping("/cluster")
    public ApiResult<ClusterView> cluster() {
        Node self = cluster.self();
        Node leader = cluster.leader();
        List<NodeView> members = cluster.members().stream()
                .map(n -> NodeView.of(n, self, leader)).toList();
        return ApiResult.ok(new ClusterView(cluster.clusterName(), NodeView.of(self, self, leader),
                members, cache.keyCount()));
    }

    /**
     * @param cacheKeys keys held by the answering node's replica
     */
    public record ClusterView(String clusterName, NodeView self, List<NodeView> members,
            int cacheKeys) {
    }

    /**
     * @param serverPort the node's HTTP port, published by openspreader in the node metadata
     */
    public record NodeView(String id, String name, String host, int port, String serverPort,
            String state, long startTime, boolean leader, boolean self) {

        static NodeView of(Node n, Node self, Node leader) {
            return new NodeView(n.id(), n.name(), n.host(), n.port(),
                    n.metadata(WebAddressMetadataListener.SERVER_PORT), String.valueOf(n.state()),
                    n.startTime(), leader != null && leader.id().equals(n.id()),
                    self.id().equals(n.id()));
        }
    }
}
