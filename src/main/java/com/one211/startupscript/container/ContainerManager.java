package com.one211.startupscript.container;

import com.one211.startupscript.config.AppConfig;
import com.one211.startupscript.util.PortUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;

import java.time.Duration;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ContainerManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(ContainerManager.class);

    // Keep track of all containers for shutdown
    private final Set<GenericContainer<?>> containers = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final AppConfig config;

    public ContainerManager(AppConfig config) {
        this.config = config;
    }

    public GenericContainer<?> startContainer() {
        // Dynamic internal ports
        int flightPort = PortUtils.getUnusedPort();
        int httpPort = PortUtils.getUnusedPort();

        GenericContainer<?> container = new GenericContainer<>(config.getImage())
                .withExposedPorts(flightPort, httpPort)
                .withCommand("--conf", "flight-sql.port=" + flightPort)
                .withLogConsumer(new Slf4jLogConsumer(LOGGER))
                .waitingFor(
                        Wait.forLogMessage(".*Flight Server is up.*", 1)
                                .withStartupTimeout(Duration.ofSeconds(60))
                );

        container.start();
        containers.add(container);

        LOGGER.info("Container started: flightPort={} hostPort={}", flightPort, container.getMappedPort(flightPort));

        return container;
    }

    public String buildJdbcUrl(GenericContainer<?> container, int internalPort) {
        int hostPort = container.getMappedPort(internalPort);
        return String.format(
                "jdbc:arrow-flight-sql://%s:%d?database=memory&user=%s&password=%s&disableCertificateVerification=true",
                container.getHost(),
                hostPort,
                config.getUser(),
                config.getPassword()
        );
    }

    /**
     * Stop all containers
     */
    public void shutdown() {
        LOGGER.info("Stopping all containers...");
        for (GenericContainer<?> container : containers) {
            try {
                container.stop();
            } catch (Exception e) {
                LOGGER.warn("Failed to stop container: {}", container, e);
            }
        }
        containers.clear();
    }
}
