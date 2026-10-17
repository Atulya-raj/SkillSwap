package com.skillswap.exception;

/**
 * Checked base exception for all SkillSwap business and domain errors.
 */
public class SkillSwapException extends Exception {

    private static final long serialVersionUID = 1L;

    /**
     * Constructs a SkillSwapException with the given user-friendly message.
     *
     * @param message the user-friendly message
     */
    public SkillSwapException(String message) {
        super(message);
    }

    /**
     * Constructs a SkillSwapException with message and cause.
     *
     * @param message the detail message
     * @param cause the underlying cause
     */
    public SkillSwapException(String message, Throwable cause) {
        super(message, cause);
    }
}
