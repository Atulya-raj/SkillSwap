package com.skillswap.exception;

/**
 * Exception thrown when a requested domain entity cannot be located.
 */
public class ResourceNotFoundException extends SkillSwapException {

    private static final long serialVersionUID = 1L;

    /**
     * Constructs a ResourceNotFoundException with a detail message.
     *
     * @param message the detail message
     */
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
