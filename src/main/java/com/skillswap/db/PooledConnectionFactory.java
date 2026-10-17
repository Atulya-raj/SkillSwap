package com.skillswap.db;

import com.skillswap.exception.DataAccessException;
import com.skillswap.util.Config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Factory for creating physical JDBC database connections.
 */
public class PooledConnectionFactory {

    private static final Logger LOGGER = Logger.getLogger(PooledConnectionFactory.class.getName());

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            LOGGER.log(Level.SEVERE, "MySQL JDBC Driver not found", e);
        }
    }

    /**
     * Creates a new physical JDBC connection using current configuration.
     *
     * @return a new active JDBC Connection
     * @throws DataAccessException if connection fails
     */
    public Connection createConnection() {
        String url = Config.getString("db.url");
        String user = Config.getString("db.user");
        String password = Config.getString("db.password");
        try {
            return DriverManager.getConnection(url, user, password);
        } catch (SQLException e) {
            throw new DataAccessException("Failed to connect to database at: " + url, e);
        }
    }
}
