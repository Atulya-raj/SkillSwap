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
     */
    public static <T> T execute(TxWork<T> work) throws com.skillswap.exception.SkillSwapException {
        if (CURRENT_TX_CONNECTION.get() != null) {
            // Already inside an active transaction, join it
            try {
                return work.run();
            } catch (com.skillswap.exception.SkillSwapException e) {
                throw e;
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new DataAccessException("Transactional work failed", e);
            }
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
            if (t instanceof com.skillswap.exception.SkillSwapException) {
                throw (com.skillswap.exception.SkillSwapException) t;
            } else if (t instanceof RuntimeException) {
                throw (RuntimeException) t;
            } else if (t instanceof Error) {
                throw (Error) t;
            } else {
                throw new DataAccessException("Transaction failed due to checked exception", t);
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
