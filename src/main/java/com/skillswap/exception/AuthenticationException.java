package com.skillswap.exception;

/**
 * Exception thrown when authentication fails due to invalid credentials or inactive account status.
 */
public class AuthenticationException extends SkillSwapException {

    private static final long serialVersionUID = 1L;

    /**
     * Constructs an AuthenticationException with a detail message.
     *
     * @param message the detail message
     */
    public AuthenticationException(String message) {
        super(message);
    }
}
