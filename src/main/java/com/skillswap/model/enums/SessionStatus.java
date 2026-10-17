package com.skillswap.model.enums;

import java.util.EnumSet;
import java.util.Set;

/**
 * Lifecycle states of a skill swap session request.
 */
public enum SessionStatus {
    PENDING,
    ACCEPTED,
    REJECTED,
    CANCELLED,
    EXPIRED,
    COMPLETED,
    NO_SHOW_LEARNER,
    NO_SHOW_PROVIDER;

    private static final Set<SessionStatus> TERMINAL_STATES = EnumSet.of(
            REJECTED, CANCELLED, EXPIRED, COMPLETED, NO_SHOW_LEARNER, NO_SHOW_PROVIDER
    );

    /**
     * Checks if this status is a terminal state in the session lifecycle.
     *
     * @return true if terminal, false otherwise
     */
    public boolean isTerminal() {
        return TERMINAL_STATES.contains(this);
    }

    /**
     * Evaluates if transitioning from this status to next status is permitted.
     *
     * @param next the candidate target status
     * @return true if transition is valid, false otherwise
     */
    public boolean canMoveTo(SessionStatus next) {
        if (next == null) {
            return false;
        }
        return switch (this) {
            case PENDING -> next == ACCEPTED || next == REJECTED || next == CANCELLED || next == EXPIRED;
            case ACCEPTED -> next == COMPLETED || next == CANCELLED || next == NO_SHOW_LEARNER || next == NO_SHOW_PROVIDER;
            default -> false;
        };
    }
}
