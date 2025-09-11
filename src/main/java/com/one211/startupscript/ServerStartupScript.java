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
        // Merge application.conf + system properties (from -D flags)
        Config config = Config.builder()
                .sources(
                        ConfigSources.classpath("application.conf"),
                        ConfigSources.systemProperties()
                )
                .build();

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

        String protocol = appConfig.isTlsEnabled() ? "https" : "http";
        String host = "0.0.0.0";
        String reachableHost = config.get("app.reachable-host")
                .asString()
                .orElse(InetAddress.getLocalHost().getHostAddress());
        int port = server.port();

        LOGGER.info("Server bound to {}://{}:{}", protocol, host, port);
        LOGGER.info("Server accessible at {}://{}:{}", protocol, reachableHost, port);
        System.out.printf("Server started at %s://%s:%d%n", protocol, reachableHost, port);

        Runtime.getRuntime().addShutdownHook(new Thread(containerManager::shutdown));

    }
}
