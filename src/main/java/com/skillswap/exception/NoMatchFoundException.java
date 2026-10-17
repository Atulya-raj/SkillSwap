package com.skillswap.exception;

/**
 * Exception thrown when the matching engine finds no compatible providers.
 */
public class NoMatchFoundException extends SkillSwapException {

    private static final long serialVersionUID = 1L;

    /**
     * Constructs a NoMatchFoundException with a detail message.
     *
     * @param message the detail message
     */
    public NoMatchFoundException(String message) {
        super(message);
    }
}
