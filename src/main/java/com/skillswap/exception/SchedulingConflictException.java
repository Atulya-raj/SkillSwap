package com.skillswap.exception;

/**
 * Exception thrown when a session proposal conflicts with existing accepted sessions.
 */
public class SchedulingConflictException extends SkillSwapException {

    private static final long serialVersionUID = 1L;

    /**
     * Constructs a SchedulingConflictException with a detail message.
     *
     * @param message the detail message
     */
    public SchedulingConflictException(String message) {
        super(message);
    }
}
