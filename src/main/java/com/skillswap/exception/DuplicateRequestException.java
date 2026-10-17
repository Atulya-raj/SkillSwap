package com.skillswap.exception;

/**
 * Exception thrown when an active request between learner, provider, and skill already exists.
 */
public class DuplicateRequestException extends SkillSwapException {

    private static final long serialVersionUID = 1L;

    /**
     * Constructs a DuplicateRequestException with a detail message.
     *
     * @param message the detail message
     */
    public DuplicateRequestException(String message) {
        super(message);
    }
}
