package com.skillswap.exception;

/**
 * Exception thrown when a user has insufficient credits to request a session.
 */
public class InsufficientCreditsException extends SkillSwapException {

    private static final long serialVersionUID = 1L;

    /**
     * Constructs an InsufficientCreditsException with a detail message.
     *
     * @param message the detail message
     */
    public InsufficientCreditsException(String message) {
        super(message);
    }
}
