package com.one211.startupscript.service;

import com.one211.startupscript.model.StatementResult;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SqlExecutor {
    private static final java.util.logging.Logger LOGGER = Logger.getLogger(SqlExecutor.class.getName());

    public static StatementResult executeSingle(String jdbcUrl, String script) {
        if (script == null || script.trim().isEmpty()) {
            return null;
        }
        try (Connection conn = DriverManager.getConnection(jdbcUrl);
             Statement stmt = conn.createStatement()) {
            stmt.execute(script);
            LOGGER.info("SQL executed successfully and returned a ResultSet: " + script);
            return new StatementResult(script, true);
        } catch (Exception ex) {
            LOGGER.log(Level.parse("Failed to execute script: {}"), script, ex);
            return new StatementResult(script, false, ex.getMessage());
        }
    }
}