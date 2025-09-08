package com.one211.startupscript;

import com.one211.startupscript.auth.AuthFilter;
import com.one211.startupscript.auth.AuthService;
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

        // Core services
        ContainerManager containerManager = new ContainerManager(config);
        ClusterService clusterService = new ClusterService(containerManager);
        ClusterController controller = new ClusterController(clusterService);

        // Auth service
        AuthService authService = new AuthService(config.getAuthUser(), config.getAuthPassword());

        // Build server with authentication filter + controller
        WebServer server = WebServer.builder()
                .routing(r -> {
                    // Apply AuthFilter globally
                    r.addFilter(new AuthFilter(authService));
                    controller.register(r);
                })
                .port(config.getServerPort())
                .build();

        server.start();
        LOGGER.info("Helidon server running at http://localhost:" + config.getServerPort());
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOGGER.info("Shutting down...");
            containerManager.shutdown();
        }));
    }
}
