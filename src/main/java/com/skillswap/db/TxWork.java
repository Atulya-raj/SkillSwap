package com.skillswap.db;

/**
 * Functional interface for units of transactional work.
 *
 * @param <T> the result type returned by the transaction
 */
@FunctionalInterface
public interface TxWork<T> {

    /**
     * Executes work within a transaction.
     *
     * @return the result of the work
     * @throws Exception if any error occurs
     */
    T run() throws Exception;
}
