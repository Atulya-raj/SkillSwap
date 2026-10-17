package com.skillswap.db;

import com.skillswap.exception.DataAccessException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Initializes database schema and seed data at application startup.
 */
public class DatabaseInitializer {

    private static final Logger LOGGER = Logger.getLogger(DatabaseInitializer.class.getName());

    /**
     * Runs schema and seed initialization against the database.
     */
    public void initialize() {
        try (Connection conn = Database.acquire()) {
            initialize(conn);
        } catch (SQLException e) {
            throw new DataAccessException("Failed to initialize database", e);
        }
    }

    /**
     * Runs schema and seed initialization against the provided connection.
     *
     * @param conn the active JDBC connection
     */
    public void initialize(Connection conn) {
        LOGGER.log(Level.INFO, "Starting database schema initialization...");
        executeSqlScript(conn, "db/schema.sql");

        if (isCategoriesEmpty(conn)) {
            LOGGER.log(Level.INFO, "Categories table is empty. Executing seed data...");
            executeSqlScript(conn, "db/seed.sql");
            LOGGER.log(Level.INFO, "Seed data inserted successfully.");
        } else {
            LOGGER.log(Level.INFO, "Database already contains seed data; skipping seed.sql.");
        }
    }

    private boolean isCategoriesEmpty(Connection conn) {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM categories")) {
            if (rs.next()) {
                return rs.getInt(1) == 0;
            }
            return true;
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Could not check categories count: {0}", e.getMessage());
            return false;
        }
    }

    private void executeSqlScript(Connection conn, String resourcePath) {
        List<String> statements = loadStatements(resourcePath);
        for (String sql : statements) {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(sql);
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Failed executing SQL: " + sql, e);
                throw new DataAccessException("Error executing script " + resourcePath + ": " + sql, e);
            }
        }
    }

    /**
     * Reads a SQL script from classpath and splits it into individual executable statements.
     *
     * @param resourcePath classpath path to SQL script
     * @return list of SQL statement strings
     */
    public List<String> loadStatements(String resourcePath) {
        List<String> statements = new ArrayList<>();
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new DataAccessException("SQL resource not found on classpath: " + resourcePath);
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                StringBuilder currentStmt = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    String trimmed = line.trim();
                    // Skip full-line comments and empty lines
                    if (trimmed.isEmpty() || trimmed.startsWith("--") || trimmed.startsWith("//")) {
                        continue;
                    }
                    // Handle inline comments
                    int commentIndex = line.indexOf("--");
                    if (commentIndex >= 0) {
                        line = line.substring(0, commentIndex);
                    }
                    currentStmt.append(line).append(" ");
                    if (trimmed.endsWith(";")) {
                        String stmtStr = currentStmt.toString().trim();
                        if (stmtStr.endsWith(";")) {
                            stmtStr = stmtStr.substring(0, stmtStr.length() - 1).trim();
                        }
                        if (!stmtStr.isEmpty()) {
                            statements.add(stmtStr);
                        }
                        currentStmt.setLength(0);
                    }
                }
                String remaining = currentStmt.toString().trim();
                if (!remaining.isEmpty()) {
                    if (remaining.endsWith(";")) {
                        remaining = remaining.substring(0, remaining.length() - 1).trim();
                    }
                    if (!remaining.isEmpty()) {
                        statements.add(remaining);
                    }
                }
            }
        } catch (IOException e) {
            throw new DataAccessException("Failed reading SQL resource: " + resourcePath, e);
        }
        return statements;
    }
}
