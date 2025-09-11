package com.one211.startupscript.util;

import com.one211.startupscript.auth.AuthFilter;
import com.one211.startupscript.auth.AuthService;
import com.one211.startupscript.config.AppConfig;
import com.one211.startupscript.container.ContainerManager;
import com.one211.startupscript.controller.ClusterController;
import com.one211.startupscript.service.ClusterService;
import io.helidon.config.Config;
import io.helidon.config.ConfigSources;
import io.helidon.webserver.WebServer;
import org.junit.jupiter.api.*;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.*;

class ServerStartupScriptIntegrationTest {

    static WebServer server;
    static HttpClient httpsClient;

    @BeforeAll
    static void setup() throws Exception {
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

        server = WebServer.builder()
                .config(config.get("server")) // TLS + port from config
                .routing(r -> {
                    r.addFilter(new AuthFilter(authService));
                    controller.register(r);
                })
                .build()
                .start();

        httpsClient = HttpClient.newBuilder()
                .sslContext(trustAllSslContext())
                .build();
    }

    @AfterAll
    static void stopServer() {
        if (server != null) {
            server.stop();
        }
    }
//need trustAllSslContext() in your integration tests because your Helidon server is using a self-signed certificate,
// and without it, your HttpClient will throw SSLHandshakeException.
    private static SSLContext trustAllSslContext() throws Exception {
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, new TrustManager[]{new X509TrustManager() {
            public void checkClientTrusted(X509Certificate[] chain, String authType) {}
            public void checkServerTrusted(X509Certificate[] chain, String authType) {}
            public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
        }}, new SecureRandom());
        return sslContext;
    }

    private static String basicAuth(String user, String pass) {
        return "Basic " + Base64.getEncoder().encodeToString((user + ":" + pass).getBytes());
    }

    private URI uri(String path) {
        return URI.create("https://localhost:" + server.port() + path);
    }

    @Test
    void testClusterEndpointWithValidAuth() throws Exception {
        String jsonBody = """
            { "clusterName": "SecureCluster", "startUpScript": "CREATE TABLE secure (id INT);" }
            """;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri("/cluster?orgId=org-secure"))
                .header("Content-Type", "application/json")
                .header("Authorization", basicAuth("admin", "admin"))
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response = httpsClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("SecureCluster"));
        assertTrue(response.body().contains("org-secure"));
    }

    @Test
    void testClusterEndpointWithoutAuth() throws Exception {
        String jsonBody = "{ \"clusterName\": \"NoAuthCluster\", \"startUpScript\": \"CREATE TABLE noauth(id INT);\" }";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri("/cluster?orgId=org-noauth"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response = httpsClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(401, response.statusCode());
        assertTrue(response.body().contains("Unauthorized"));
    }

    @Test
    void testClusterEndpointWithInvalidAuth() throws Exception {
        String jsonBody = "{ \"clusterName\": \"BadAuthCluster\", \"startUpScript\": \"CREATE TABLE bad(id INT);\" }";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri("/cluster?orgId=org-badauth"))
                .header("Content-Type", "application/json")
                .header("Authorization", basicAuth("wrong", "creds"))
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response = httpsClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(401, response.statusCode());
        assertTrue(response.body().contains("Unauthorized"));
    }

    @Test
    void testClusterEndpointMissingOrgId() throws Exception {
        String jsonBody = "{ \"clusterName\": \"MissingOrg\", \"startUpScript\": \"CREATE TABLE missing(id INT);\" }";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri("/cluster")) // missing orgId
                .header("Content-Type", "application/json")
                .header("Authorization", basicAuth("admin", "admin"))
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response = httpsClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(400, response.statusCode());
        assertTrue(response.body().contains("Missing orgId"));
    }

    @Test
    void testMultipleSequentialRequests() throws Exception {
        for (int i = 1; i <= 3; i++) {
            String jsonBody = """
            { "clusterName": "Cluster-%d", "startUpScript": "CREATE TABLE t%d(id INT);" }
            """.formatted(i, i);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(uri("/cluster?orgId=org-" + i))
                    .header("Content-Type", "application/json")
                    .header("Authorization", basicAuth("admin", "admin"))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = httpsClient.send(request, HttpResponse.BodyHandlers.ofString());

            assertEquals(200, response.statusCode(), "Request " + i + " failed");
            assertTrue(response.body().contains("Cluster-" + i));
            assertTrue(response.body().contains("org-" + i));
        }
    }

    @Test
    void testMultipleParallelRequests() throws Exception {
        var executor = Executors.newFixedThreadPool(3);
        var futures = new ArrayList<Future<HttpResponse<String>>>();

        for (int i = 1; i <= 3; i++) {
            int finalI = i;
            futures.add(executor.submit(() -> {
                String jsonBody = """
                { "clusterName": "Cluster-%d", "startUpScript": "CREATE TABLE t%d(id INT);" }
                """.formatted(finalI, finalI);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(uri("/cluster?orgId=org-" + finalI))
                        .header("Content-Type", "application/json")
                        .header("Authorization", basicAuth("admin", "admin"))
                        .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                        .build();

                return httpsClient.send(request, HttpResponse.BodyHandlers.ofString());
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
}
