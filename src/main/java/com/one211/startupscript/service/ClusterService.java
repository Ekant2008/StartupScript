package com.one211.startupscript.service;

import com.one211.startupscript.container.ContainerManager;
import com.one211.startupscript.model.ClusterRequest;
import com.one211.startupscript.model.ClusterResponse;
import com.one211.startupscript.model.StatementResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.testcontainers.containers.GenericContainer;

import java.util.List;

public class ClusterService {

    private final ContainerManager containerManager;
    private final ObjectMapper mapper = new ObjectMapper();

    public ClusterService(ContainerManager containerManager) {
        this.containerManager = containerManager;
    }

    public ClusterResponse processRequest(String orgId, String body) throws Exception {
        ClusterRequest request = mapper.readValue(body, ClusterRequest.class);
        String clusterName = request.clusterName();
        String script = request.startUpScript();
        if (clusterName == null || clusterName.isBlank() || script == null || script.isBlank()) {
            throw new IllegalArgumentException("Missing required fields: clusterName or startUpScript");
        }

        GenericContainer<?> container = containerManager.startContainer();
        int flightPort = container.getExposedPorts().getFirst();
        String jdbcUrl = containerManager.buildJdbcUrl(container, flightPort);

        StatementResult result = SqlExecutor.executeSingle(jdbcUrl, script);

        return new ClusterResponse(
                orgId,
                clusterName,
                "Flight SQL running",
                container.getHost() + ":" + container.getMappedPort(flightPort),
                List.of(result)
        );
    }
}
