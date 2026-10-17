package com.skillswap.badge;

import com.skillswap.model.User;

/**
 * Strategy interface for checking if a user qualifies for a badge.
 */
public interface BadgeRule {

    /**
     * Unique code of the badge evaluated by this rule.
     *
     * @return badge code (e.g. FIRST_SWAP)
     */
    String code();

    /**
     * Evaluates if the user satisfies the criteria for this badge.
     *
     * @param user user entity
     * @param stats calculated badge statistics
     * @return true if user qualifies, false otherwise
     */
    boolean qualifies(User user, BadgeStats stats);
}
