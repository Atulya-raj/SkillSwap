package com.skillswap.exception;

/**
 * Unchecked exception wrapping database access and SQL failures.
 */
public class DataAccessException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Constructs a DataAccessException with a detail message.
     *
     * @param message the detail message
     */
    public DataAccessException(String message) {
        super(message);
    }

    /**
     * Constructs a DataAccessException with a detail message and cause.
     *
     * @param message the detail message
     * @param cause the underlying cause
     */
    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a DataAccessException with a cause.
     *
     * @param cause the underlying cause
     */
    public DataAccessException(Throwable cause) {
        super(cause);
    }
}
