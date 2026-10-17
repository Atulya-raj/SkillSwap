package com.skillswap.exception;

/**
 * Exception thrown when a user attempts an action outside their granted role or on another's resource.
 */
public class AuthorizationException extends SkillSwapException {

    private static final long serialVersionUID = 1L;

    /**
     * Constructs an AuthorizationException with a detail message.
     *
     * @param message the detail message
     */
    public AuthorizationException(String message) {
        super(message);
    }
}
