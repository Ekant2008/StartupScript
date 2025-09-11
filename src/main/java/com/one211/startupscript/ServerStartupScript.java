package com.one211.startupscript;

import com.one211.startupscript.auth.AuthFilter;
import com.one211.startupscript.auth.AuthService;
import com.one211.startupscript.config.AppConfig;
import com.one211.startupscript.container.ContainerManager;
import com.one211.startupscript.controller.ClusterController;
import com.one211.startupscript.service.ClusterService;
import io.helidon.config.Config;
import io.helidon.config.ConfigSources;
import io.helidon.webserver.WebServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class ServerStartupScript {
    private static final Logger LOGGER = LoggerFactory.getLogger(ServerStartupScript.class);

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