package com.skillswap.db;

import com.skillswap.exception.DataAccessException;
import com.skillswap.util.Config;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Hand-written database connection pool and connection dispenser.
 */
public final class Database {

    private static final Logger LOGGER = Logger.getLogger(Database.class.getName());
    private static volatile Database instance;

    private final ArrayBlockingQueue<Connection> pool;
    private final PooledConnectionFactory factory;
    private final int poolSize;
    private final long timeoutMs;
    private final AtomicBoolean isShutdown = new AtomicBoolean(false);

    private Database() {
        this.poolSize = Config.getInt("pool.size", 10);
        this.timeoutMs = Config.getLong("pool.timeout.ms", 5000L);
        this.factory = new PooledConnectionFactory();
        this.pool = new ArrayBlockingQueue<>(poolSize);
        initializePool();
    }

    /**
     * Returns the singleton Database instance, creating it if necessary.
     *
     * @return singleton instance
     */
    public static Database getInstance() {
        if (instance == null) {
            synchronized (Database.class) {
                if (instance == null) {
                    instance = new Database();
                }
            }
        }
        return instance;
    }

    /**
     * Initializes or re-initializes the connection pool with current configuration.
     */
    public static synchronized void init() {
        if (instance != null) {
            instance.shutdown();
            instance = null;
        }
        getInstance();
    }

    private void initializePool() {
        for (int i = 0; i < poolSize; i++) {
            try {
                Connection conn = factory.createConnection();
                pool.offer(conn);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Failed to initialize connection " + (i + 1) + " of " + poolSize, e);
                throw new DataAccessException("Failed to initialize database connection pool", e);
            }
        }
        LOGGER.log(Level.INFO, "Database connection pool initialized with {0} connections", poolSize);
    }

    /**
     * Acquires a database connection. If a transaction is active on the current thread,
     * returns the transaction connection; otherwise borrows one from the pool.
     *
     * @return an active Connection proxy
     * @throws DataAccessException if acquiring times out or fails
     */
    public static Connection acquire() {
        return getInstance().getConnection();
    }

    /**
     * Acquires a database connection from this instance.
     *
     * @return an active Connection proxy
     */
    public Connection getConnection() {
        if (isShutdown.get()) {
            throw new DataAccessException("Connection pool is shut down");
        }

        Connection txConn = TransactionManager.getCurrentConnection();
        if (txConn != null) {
            return txConn;
        }

        Connection rawConn = borrowRawConnection();
        return wrapBorrowedConnection(rawConn);
    }

    /**
     * Borrows a raw physical connection from the pool, validating and replacing if dead.
     *
     * @return a valid physical Connection
     */
    public Connection borrowRawConnection() {
        if (isShutdown.get()) {
            throw new DataAccessException("Connection pool is shut down");
        }

        try {
            Connection rawConn = pool.poll(timeoutMs, TimeUnit.MILLISECONDS);
            if (rawConn == null) {
                throw new DataAccessException("Connection pool exhausted: timed out waiting for connection after " + timeoutMs + " ms");
            }

            // Validate with isValid(2); replace if dead
            if (!rawConn.isValid(2)) {
                try {
                    rawConn.close();
                } catch (SQLException ignored) {
                }
                rawConn = factory.createConnection();
            }

            return rawConn;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DataAccessException("Interrupted while waiting for database connection", e);
        } catch (SQLException e) {
            throw new DataAccessException("Error validating connection", e);
        }
    }

    /**
     * Returns a raw connection to the pool.
     *
     * @param raw the physical connection to return
     */
    public void returnRawConnection(Connection raw) {
        if (raw == null) {
            return;
        }

        if (isShutdown.get()) {
            try {
                raw.close();
            } catch (SQLException ignored) {
            }
            return;
        }

        try {
            if (!raw.isClosed() && raw.isValid(1)) {
                if (!pool.offer(raw)) {
                    // Pool was full, close excess connection
                    raw.close();
                }
            } else {
                try {
                    raw.close();
                } catch (SQLException ignored) {
                }
                // Replenish the pool with a fresh connection
                pool.offer(factory.createConnection());
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error returning connection to pool", e);
            try {
                raw.close();
            } catch (SQLException ignored) {
            }
        }
    }

    /**
     * Wraps a raw connection in a dynamic proxy that returns it to the pool on close().
     */
    private Connection wrapBorrowedConnection(Connection raw) {
        final AtomicBoolean closed = new AtomicBoolean(false);

        InvocationHandler handler = (proxy, method, args) -> {
            String methodName = method.getName();
            if ("close".equals(methodName)) {
                if (closed.compareAndSet(false, true)) {
                    returnRawConnection(raw);
                }
                return null;
            }
            if ("isClosed".equals(methodName)) {
                return closed.get() || raw.isClosed();
            }
            if (closed.get()) {
                throw new SQLException("Connection has already been closed");
            }
            try {
                return method.invoke(raw, args);
            } catch (java.lang.reflect.InvocationTargetException e) {
                throw e.getTargetException();
            }
        };

        return (Connection) Proxy.newProxyInstance(
                Connection.class.getClassLoader(),
                new Class<?>[]{Connection.class},
                handler
        );
    }

    /**
     * Closes all connections and shuts down the connection pool.
     */
    public synchronized void shutdown() {
        if (isShutdown.compareAndSet(false, true)) {
            LOGGER.log(Level.INFO, "Shutting down database connection pool...");
            Connection conn;
            while ((conn = pool.poll()) != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    LOGGER.log(Level.WARNING, "Error closing connection on shutdown", e);
                }
            }
        }
    }

    /**
     * Returns the number of currently available connections in the pool.
     *
     * @return available connection count
     */
    public int getAvailableConnections() {
        return pool.size();
    }
}
