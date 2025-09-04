package com.one211.startupscript.util;

import com.one211.startupscript.config.AppConfig;
import com.one211.startupscript.container.ContainerManager;
import com.one211.startupscript.controller.ClusterController;
import com.one211.startupscript.service.ClusterService;
import io.helidon.webserver.WebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.*;

class ServerStartupScriptIntegrationTest {

    static WebServer server;
    static ContainerManager containerManager;

    @BeforeAll
    static void setup() {
        // Use real AppConfig and ContainerManager
        AppConfig config = new AppConfig(); // set image, user, password in AppConfig if needed
        containerManager = new ContainerManager(config);
        ClusterService clusterService = new ClusterService(containerManager);
        ClusterController controller = new ClusterController(clusterService);

        // Start Helidon server on dynamic port
        server = WebServer.builder()
                .routing(r -> controller.register(r))
                .port(0)
                .build()
                .start()
                ;
    }
    @AfterAll
    static void stopServer() {
        if (server != null) {
            server.stop();
        }
    }
    @Test
    void testClusterEndpointSuccess() throws Exception {
        int port = server.port();
        HttpClient client = HttpClient.newHttpClient();

        String jsonBody = """
                {
                  "clusterName": "IntegrationCluster",
                  "startUpScript": "CREATE TABLE abc(id INT);"
                }
                """;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI("http://localhost:" + port + "/?orgId=org-1"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("IntegrationCluster")); // clusterName should be in response
    }

    @Test
    void testClusterEndpointMissingOrgId() throws Exception {
        int port = server.port();
        HttpClient client = HttpClient.newHttpClient();

        String jsonBody = """
                {
                  "clusterName": "IntegrationCluster",
                  "startUpScript": "CREATE TABLE abc(id INT);"
                }
                """;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI("http://localhost:" + port + "/")) // missing orgId
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(400, response.statusCode());
        assertTrue(response.body().contains("Missing orgId"));
    }
    @Test
    void testMultipleSequentialRequests() throws Exception {
        int port = server.port();
        HttpClient client = HttpClient.newHttpClient();

        for (int i = 1; i <= 3; i++) {
            String jsonBody = """
                {
                  "clusterName": "Cluster-%d",
                  "startUpScript": "CREATE TABLE t%d(id INT);"
                }
                """.formatted(i, i);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI("http://localhost:" + port + "/?orgId=org-" + i))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            assertEquals(200, response.statusCode(), "Request " + i + " failed");
            assertTrue(response.body().contains("Cluster-" + i));
            assertTrue(response.body().contains("org-" + i));
        }
    }
    @Test
    void testMultipleParallelRequests() throws Exception {
        int port = server.port();
        HttpClient client = HttpClient.newHttpClient();

        var executor = java.util.concurrent.Executors.newFixedThreadPool(3);
        var futures = new java.util.ArrayList<java.util.concurrent.Future<HttpResponse<String>>>();

        for (int i = 1; i <= 3; i++) {
            int finalI = i;
            futures.add(executor.submit(() -> {
                String jsonBody = """
                    {
                      "clusterName": "Cluster-%d",
                      "startUpScript": "CREATE TABLE t%d(id INT);"
                    }
                    """.formatted(finalI, finalI);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(new URI("http://localhost:" + port + "/?orgId=org-" + finalI))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                        .build();

                return client.send(request, HttpResponse.BodyHandlers.ofString());
            }));
        }

        for (int i = 0; i < futures.size(); i++) {
            HttpResponse<String> response = futures.get(i).get(); // wait for completion
            assertEquals(200, response.statusCode(), "Parallel request " + (i + 1) + " failed");
            assertTrue(response.body().contains("Cluster-" + (i + 1)));
            assertTrue(response.body().contains("org-" + (i + 1)));
        }

        executor.shutdown();
    }


}

