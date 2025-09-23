package com.one211.startupscript.util;

import com.one211.startupscript.auth.AuthService;
import com.one211.startupscript.auth.AuthFilter;
import com.one211.startupscript.config.AppConfig;
import com.one211.startupscript.container.ContainerManager;
import com.one211.startupscript.controller.ClusterController;
import com.one211.startupscript.service.ClusterService;
import io.helidon.config.Config;
import io.helidon.webserver.WebServer;
import org.junit.jupiter.api.*;
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
        Config config = Config.create();

        AppConfig appConfig = new AppConfig(config);

        ContainerManager containerManager = new ContainerManager(appConfig);
        ClusterService clusterService = new ClusterService(containerManager);
        ClusterController controller = new ClusterController(clusterService);

        AuthService authService = new AuthService(appConfig.getAuthUser(), appConfig.getAuthPassword());

        server = WebServer.builder()
                .routing(r -> {
                    r.addFilter(new AuthFilter(authService));
                    controller.register(r);
                })
                .port(0)
                .build()
                .start();
    }

    @AfterAll
    static void stopServer() {
        if (server != null) {
            server.stop();
        }
    }

    @Test
    void testClusterEndpointWithValidAuth() throws Exception {
        int port = server.port();
        HttpClient client = HttpClient.newHttpClient();

        String jsonBody = """
            {
              "clusterName": "SecureCluster",
              "startUpScript": "CREATE TABLE secure (id INT, name VARCHAR);"
            }
            """;

        String basicAuth = java.util.Base64.getEncoder()
                .encodeToString("admin:admin".getBytes());

        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI("http://localhost:" + port + "/cluster?orgId=org-secure"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Basic " + basicAuth)
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("SecureCluster"));
        assertTrue(response.body().contains("org-secure"));
    }

    @Test
    void testClusterEndpointWithoutAuth() throws Exception {
        int port = server.port();
        HttpClient client = HttpClient.newHttpClient();

        String jsonBody = """
            {
              "clusterName": "NoAuthCluster",
              "startUpScript": "CREATE TABLE noauth (id INT, name VARCHAR);"
            }
            """;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI("http://localhost:" + port + "/cluster?orgId=org-noauth"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(401, response.statusCode());
        assertTrue(response.body().contains("Unauthorized"));
    }

    @Test
    void testClusterEndpointWithInvalidAuth() throws Exception {
        int port = server.port();
        HttpClient client = HttpClient.newHttpClient();
        String jsonBody = """
            {
              "clusterName": "BadAuthCluster",
              "startUpScript": "CREATE TABLE bad (id INT, name VARCHAR);"
            }
            """;
        String badAuth = java.util.Base64.getEncoder()
                .encodeToString("wrong:creds".getBytes());
        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI("http://localhost:" + port + "/cluster?orgId=org-badauth"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Basic " + badAuth)
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(401, response.statusCode());
        assertTrue(response.body().contains("Unauthorized"));
    }

    @Test
    void testClusterEndpointMissingOrgId() throws Exception {
        int port = server.port();
        HttpClient client = HttpClient.newHttpClient();
        String jsonBody = """
            {
              "clusterName": "MissingOrg",
              "startUpScript": "CREATE TABLE missing (id INT, name VARCHAR);"
            }
            """;
        String basicAuth = java.util.Base64.getEncoder()
                .encodeToString("admin:admin".getBytes());
        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI("http://localhost:" + port + "/cluster")) // missing orgId
                .header("Content-Type", "application/json")
                .header("Authorization", "Basic " + basicAuth)
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

        String basicAuth = java.util.Base64.getEncoder()
                .encodeToString("admin:admin".getBytes());

        for (int i = 1; i <= 3; i++) {
            String jsonBody = """
            {
              "clusterName": "Cluster-%d",
              "startUpScript": "CREATE TABLE t%d(id INT);"
            }
            """.formatted(i, i);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI("http://localhost:" + port + "/cluster?orgId=org-" + i))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Basic " + basicAuth)
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
        String basicAuth = java.util.Base64.getEncoder()
                .encodeToString("admin:admin".getBytes());
        var executor = java.util.concurrent.Executors.newFixedThreadPool(3);
        var futures = new java.util.ArrayList<java.util.concurrent.Future<HttpResponse<String>>>();
        for (int i = 1; i <= 3; i++) {
            int finalI = i;
            int finalI1 = i;
            futures.add(executor.submit(() -> {
                String jsonBody = """
                {
                  "clusterName": "Cluster-%d",
                  "startUpScript": "CREATE TABLE t%d(id INT);"
                }
                """.formatted(finalI, finalI1);
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(new URI("http://localhost:" + port + "/cluster?orgId=org-" + finalI))
                        .header("Content-Type", "application/json")
                        .header("Authorization", "Basic " + basicAuth)
                        .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                        .build();

                return client.send(request, HttpResponse.BodyHandlers.ofString());
            }));
        }
        for (int i = 0; i < futures.size(); i++) {
            HttpResponse<String> response = futures.get(i).get();
            assertEquals(200, response.statusCode(), "Parallel request " + (i + 1) + " failed");
            assertTrue(response.body().contains("Cluster-" + (i + 1)));
            assertTrue(response.body().contains("org-" + (i + 1)));
        }
        executor.shutdown();
    }

    @Test
    void testMultipleContainersSequentially() throws Exception {
        int port = server.port();
        HttpClient client = HttpClient.newHttpClient();
        String basicAuth = java.util.Base64.getEncoder()
                .encodeToString("admin:admin".getBytes());

        // Simulate multiple clusters/containers sequentially
        for (int i = 1; i <= 2; i++) {
            String clusterName = "ContainerCluster-" + i;
            String jsonBody = """
            {
              "clusterName": "%s",
              "startUpScript": "CREATE TABLE c%d(id INT);"
            }
            """.formatted(clusterName, i);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI("http://localhost:" + port + "/cluster?orgId=org-container-" + i))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Basic " + basicAuth)
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            assertEquals(200, response.statusCode(), "Container request " + i + " failed");
            assertTrue(response.body().contains(clusterName));
            assertTrue(response.body().contains("org-container-" + i));
        }
    }

}
