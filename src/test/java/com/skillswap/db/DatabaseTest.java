package com.skillswap.db;

import com.skillswap.exception.DataAccessException;
import com.skillswap.util.Config;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit and integration tests for Database connection pool and TransactionManager.
 */
public class DatabaseTest {

    @BeforeAll
    public static void setUp() {
        Config.loadDefault();
        Database.init();
        new DatabaseInitializer().initialize();
    }

    @AfterAll
    public static void tearDown() {
        Database.getInstance().shutdown();
    }

    @Test
    public void testPoolReturnsConnections() throws SQLException {
        Connection conn1 = Database.acquire();
        assertNotNull(conn1);
        assertFalse(conn1.isClosed());
        conn1.close();
        assertTrue(conn1.isClosed());

        // Acquiring again should succeed and connection should be open
        Connection conn2 = Database.acquire();
        assertNotNull(conn2);
        assertFalse(conn2.isClosed());
        conn2.close();
    }

    @Test
    public void testFailedTransactionRollsBack() throws SQLException {
        String testCategoryName = "TxTestCategory_" + System.currentTimeMillis();

        assertThrows(RuntimeException.class, () -> {
            TransactionManager.execute(() -> {
                try (Connection conn = Database.acquire();
                     Statement stmt = conn.createStatement()) {
                    stmt.executeUpdate("INSERT INTO categories (name, description) VALUES ('" + testCategoryName + "', 'Rollback test')");
                }
                throw new RuntimeException("Simulated transaction failure");
            });
        });

        // Verify the insert was rolled back
        try (Connection conn = Database.acquire();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM categories WHERE name = '" + testCategoryName + "'")) {
            assertTrue(rs.next());
            assertEquals(0, rs.getInt(1), "Row should have been rolled back");
        }
    }

    @Test
    public void testPoolExhaustionTimesOut() throws SQLException {
        int poolSize = Config.getInt("pool.size", 10);
        List<Connection> borrowed = new ArrayList<>();

        try {
            // Borrow all connections from the pool
            for (int i = 0; i < poolSize; i++) {
                borrowed.add(Database.getInstance().borrowRawConnection());
            }

            // Next borrow should timeout and throw DataAccessException
            long start = System.currentTimeMillis();
            DataAccessException ex = assertThrows(DataAccessException.class, () -> {
                Database.getInstance().borrowRawConnection();
            });
            long duration = System.currentTimeMillis() - start;

            assertTrue(ex.getMessage().contains("timed out") || ex.getMessage().contains("exhausted"),
                    "Exception message should mention timeout: " + ex.getMessage());
            assertTrue(duration >= 1000, "Should have waited for the configured timeout");
        } finally {
            // Return all borrowed connections back to the pool
            for (Connection conn : borrowed) {
                Database.getInstance().returnRawConnection(conn);
            }
        }
    }
}
