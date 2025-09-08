package com.one211.startupscript.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.one211.startupscript.model.ClusterResponse;
import com.one211.startupscript.service.ClusterService;
import io.helidon.webserver.http.HttpRouting;
import io.helidon.webserver.http.ServerRequest;
import io.helidon.webserver.http.ServerResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClusterController {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterController.class);
    private final ClusterService service;
    private final ObjectMapper mapper = new ObjectMapper();

    public ClusterController(ClusterService service) {
        this.service = service;
    }

    public void register(HttpRouting.Builder routing) {
        routing.post("/cluster", this::handleRequest);
    }

    private void handleRequest(ServerRequest req, ServerResponse res) {
        String orgId = req.query().first("orgId").orElse(null);
        if (orgId == null || orgId.isBlank()) {
            res.status(400).send("Missing orgId");
            return;
        }

        try {
            String body = req.content().as(String.class);
            ClusterResponse response = service.processRequest(orgId, body);

            // Serialize to JSON
            String json = mapper.writeValueAsString(response);
            res.send(json);

        } catch (Exception e) {
            LOGGER.error("Error handling request", e);
            res.status(500).send("Internal server error: " + e.getMessage());
        }
    }
}
