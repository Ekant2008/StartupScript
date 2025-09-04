package com.one211.startupscript;

import com.one211.startupscript.config.AppConfig;
import com.one211.startupscript.container.ContainerManager;
import com.one211.startupscript.controller.ClusterController;
import com.one211.startupscript.service.ClusterService;
import io.helidon.webserver.WebServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServerStartupScript {
    private static final Logger LOGGER = LoggerFactory.getLogger(ServerStartupScript.class);

    public static void main(String[] args) {
        AppConfig config = new AppConfig();
        ContainerManager containerManager = new ContainerManager(config);
        ClusterService clusterService = new ClusterService(containerManager);
        ClusterController controller = new ClusterController(clusterService);

        WebServer server = WebServer.builder()
                .routing(r -> controller.register(r))
                .port(8090)
                .build();

        server.start();
        LOGGER.info("Helidon server running at http://localhost:8080");

        // Graceful shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOGGER.info("Shutting down...");
            containerManager.shutdown();
        }));
    }
}