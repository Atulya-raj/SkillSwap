package com.skillswap.exception;

import com.skillswap.model.enums.SessionStatus;

/**
 * Exception thrown when attempting an invalid session lifecycle status transition.
 */
public class InvalidStateTransitionException extends SkillSwapException {

    private static final long serialVersionUID = 1L;

    /**
     * Constructs an InvalidStateTransitionException with a detail message.
     *
     * @param message the detail message
     */
    public InvalidStateTransitionException(String message) {
        super(message);
    }

    /**
     * Constructs an InvalidStateTransitionException for source and target statuses.
     *
     * @param from the source status
     * @param to the requested target status
     */
    public InvalidStateTransitionException(SessionStatus from, SessionStatus to) {
        super("Illegal state transition from " + from + " to " + to);
    }
}
