//package com.one211.startup.script.util;
//
//import com.one211.startup.script.container.ContainerManager;
//import com.one211.startup.script.model.ClusterResponse;
//import com.one211.startup.script.model.StatementResult;
//import com.one211.startup.script.service.ClusterService;
//import com.one211.startup.script.config.AppConfig;
//import org.junit.jupiter.api.AfterAll;
//import org.junit.jupiter.api.BeforeAll;
//import org.junit.jupiter.api.Test;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.testcontainers.containers.GenericContainer;
//import java.util.List;
//import static org.junit.jupiter.api.Assertions.*;
//
//class ClusterServiceIntegrationTest {
//
//    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterServiceIntegrationTest.class);
//    private static ContainerManager manager;
//    private static ClusterService service;
//
//    @BeforeAll
//    static void setUp() {
//        AppConfig config = new AppConfig(); // loads from application.conf
//        manager = new ContainerManager(config);
//        GenericContainer<?> container = manager.getOrStartContainer("org-xyz");
//
//        LOGGER.info("Test container started at {}:{}", container.getHost(),
//                container.getMappedPort(config.getInternalPort()));
//
//        service = new ClusterService(manager);
//    }
//    @AfterAll
//    static void tearDown() {
//        if (manager != null) {
//            manager.shutdown();
//        }
//    }
//    @Test
//    void testEndToEndClusterServiceWithOneStatements() throws Exception {
//        String body = """
//                {
//                  "clusterName": "IntegrationCluster",
//                  "startUpScript": "CREATE TABLE t1(id INT);"
//                }
//                """;
//
//        ClusterResponse response = service.processRequest("org-xyz", body);
//
//        assertAll("ClusterResponse basic checks",
//                () -> assertEquals("org-xyz", response.getOrgId()),
//                () -> assertEquals("IntegrationCluster", response.getClusterName()),
//                () -> assertEquals("Flight SQL running", response.getStatus()),
//                () -> assertNotNull(response.getEndpoint())
//        );
//        List<StatementResult> results = response.getStatements();
//        assertFalse(results.isEmpty(), "StatementResult list should not be empty");
//        for (StatementResult stmtResult : results) {
//            assertTrue(stmtResult.isSuccess(), "Statement failed: " + stmtResult.getScript());
//            assertNull(stmtResult.getError(), "Error for statement: " + stmtResult.getScript());
//        }
//    }
//    @Test
//    void testEndToEndClusterServiceWithMultipleStatements() throws Exception {
//        String body = """
//                {
//                  "clusterName": "IntegrationCluster",
//                  "startUpScript": "CREATE TABLE t1(id INT); INSERT INTO t1 VALUES (1); INSERT INTO t1 VALUES (2);"
//                }
//                """;
//        ClusterResponse response = service.processRequest("org-xyz", body);
//        assertAll("ClusterResponse basic checks",
//                () -> assertEquals("org-xyz", response.getOrgId()),
//                () -> assertEquals("IntegrationCluster", response.getClusterName()),
//                () -> assertEquals("Flight SQL running", response.getStatus()),
//                () -> assertNotNull(response.getEndpoint())
//        );
//
//        List<StatementResult> results = response.getStatements();
//        assertFalse(results.isEmpty(), "StatementResult list should not be empty");
//        StatementResult stmtResult = results.get(0);
//        assertTrue(stmtResult.isSuccess(), "Script execution failed: " + stmtResult.getScript());
//        assertNull(stmtResult.getError(), "Unexpected error: " + stmtResult.getError());
//        LOGGER.info("Startup script executed successfully: {}", stmtResult.getScript());
//    }
//    @Test
//    void testInvalidStartupScript() throws Exception {
//        String body = """
//                {
//                  "clusterName": "FailCluster",
//                  "startUpScript": "INVALID SQL STATEMENT;"
//                }
//                """;
//        ClusterResponse response = service.processRequest("org-xyz", body);
//        StatementResult stmtResult = response.getStatements().get(0);
//        assertFalse(stmtResult.isSuccess(), "Script should fail but marked success");
//        assertNotNull(stmtResult.getError(), "Error message should be present");
//        LOGGER.info("Invalid script execution error: {}", stmtResult.getError());
//    }
//    @Test
//    void testEmptyStartupScript() throws Exception {
//        String body = """
//                {
//                  "clusterName": "EmptyCluster",
//                  "startUpScript": ""
//                }
//                """;
//        ClusterResponse response = service.processRequest("org-xyz", body);
//        assertEquals(0, response.getStatements().size(), "Empty script should produce no StatementResult");
//    }
////    @Test
////    void testCreateInsertSelect() throws Exception {
////        String body = """
////            {
////              "clusterName": "IntegrationCluster",
////              "startUpScript": "CREATE TABLE abc (id INT, name VARCHAR); INSERT INTO abc VALUES (42, 'ekant');"
////            }
////            """;
////
////        ClusterResponse response = service.processRequest("org-xyz", body);
////
////        // Assert script execution success
////        StatementResult stmtResult = response.getStatements().get(0);
////        assertTrue(stmtResult.isSuccess(), "Script execution failed: " + stmtResult.getScript());
////
////        // Now verify database contents
////        GenericContainer<?> container = manager.getOrStartContainer("org-xyz");
////        String jdbcUrl = manager.buildJdbcUrl(container);
////
////        try (Connection conn = DriverManager.getConnection(jdbcUrl);
////             Statement stmt = conn.createStatement();
////             ResultSet rs = stmt.executeQuery("SELECT id, name FROM abc")) {
////
////            assertTrue(rs.next(), "Expected at least one row");
////            assertEquals(42, rs.getInt("id"));
////            assertEquals("ekant", rs.getString("name"));
////            assertFalse(rs.next(), "Expected only one row");
////        }
////    }
//
//
//}
