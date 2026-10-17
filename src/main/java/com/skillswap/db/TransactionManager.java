package com.skillswap.db;

import com.skillswap.exception.DataAccessException;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages database transactions using ThreadLocal connection binding.
 */
public final class TransactionManager {

    private static final Logger LOGGER = Logger.getLogger(TransactionManager.class.getName());
    private static final ThreadLocal<Connection> CURRENT_TX_CONNECTION = new ThreadLocal<>();

    private TransactionManager() {
    }

    /**
     * Returns the connection bound to the current transaction, or null if none.
     *
     * @return active transaction connection proxy, or null
     */
    public static Connection getCurrentConnection() {
        return CURRENT_TX_CONNECTION.get();
    }

    /**
     * Executes the given work within a transaction, committing on success
     * and rolling back on any failure.
     *
     * @param work the transactional unit of work
     * @param <T> the return type
     * @return the result of the work
     * @throws Exception if transaction fails
     */
    public static <T> T execute(TxWork<T> work) throws Exception {
        if (CURRENT_TX_CONNECTION.get() != null) {
            // Already inside an active transaction, join it
            return work.run();
        }

        Database db = Database.getInstance();
        Connection rawConn = db.borrowRawConnection();
        try {
            rawConn.setAutoCommit(false);
            Connection proxyConn = wrapTxConnection(rawConn);
            CURRENT_TX_CONNECTION.set(proxyConn);

            T result = work.run();
            rawConn.commit();
            return result;
        } catch (Throwable t) {
            try {
                rawConn.rollback();
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Rollback failed", e);
            }
            if (t instanceof Exception) {
                throw (Exception) t;
            } else {
                throw new DataAccessException("Transaction failed due to error", t);
            }
        } finally {
            CURRENT_TX_CONNECTION.remove();
            try {
                rawConn.setAutoCommit(true);
            } catch (SQLException ignored) {
            }
            db.returnRawConnection(rawConn);
        }
    }

    private static Connection wrapTxConnection(Connection raw) {
        InvocationHandler handler = (proxy, method, args) -> {
            String methodName = method.getName();
            if ("close".equals(methodName)) {
                // In transaction: close() must not close or return raw connection
                return null;
            }
            if ("isClosed".equals(methodName)) {
                return raw.isClosed();
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
}
