package com.one211.startupscript;

import com.one211.startupscript.auth.AuthFilter;
import com.one211.startupscript.auth.AuthService;
import com.one211.startupscript.config.AppConfig;
import com.one211.startupscript.container.ContainerManager;
import com.one211.startupscript.controller.ClusterController;
import com.one211.startupscript.service.ClusterService;
import io.helidon.config.Config;
import io.helidon.webserver.WebServer;

import java.net.UnknownHostException;
import java.util.logging.Logger;

public class ServerStartupScript {
    private static final Logger LOGGER = Logger.getLogger(ServerStartupScript.class.getName());

    public static void main(String[] args) throws UnknownHostException {
        Config config = Config.create();
        AppConfig appConfig = new AppConfig(config);
        ContainerManager containerManager = new ContainerManager(appConfig);
        ClusterService clusterService = new ClusterService(containerManager);
        ClusterController controller = new ClusterController(clusterService);
        AuthService authService = new AuthService(appConfig.getAuthUser(), appConfig.getAuthPassword());
        WebServer server = WebServer.builder()
                .config(config.get("server"))
                .routing(r -> {
                    r.addFilter(new AuthFilter(authService));
                    controller.register(r);
                })
                .build();

        server.start();
        Runtime.getRuntime().addShutdownHook(new Thread(containerManager::shutdown));

    }
}