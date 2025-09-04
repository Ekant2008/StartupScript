package com.one211.startupscript.util;

import com.one211.startupscript.model.StatementResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class SqlExecutor {
    private static final Logger LOGGER = LoggerFactory.getLogger(SqlExecutor.class);

    public static StatementResult executeSingle(String jdbcUrl, String script) {
        if (script == null || script.trim().isEmpty()) {
            return null;
        }
        try (Connection conn = DriverManager.getConnection(jdbcUrl);
             Statement stmt = conn.createStatement()) {
             stmt.execute(script);
             LOGGER.info("Successfully executed script: {}", script);
             return new StatementResult(script, true);
        } catch (Exception ex) {
            LOGGER.error("Failed to execute script: {}", script, ex);
            return new StatementResult(script, false, ex.getMessage());
        }
    }
}
