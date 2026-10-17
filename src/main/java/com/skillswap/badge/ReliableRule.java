package com.skillswap.badge;

import com.skillswap.model.User;

/**
 * Awards RELIABLE badge when user has completed at least 5 sessions with 0 reliability strikes.
 */
public class ReliableRule implements BadgeRule {

    @Override
    public String code() {
        return "RELIABLE";
    }

    @Override
    public boolean qualifies(User user, BadgeStats stats) {
        return stats.getTotalSessions() >= 5 && stats.getReliabilityStrikes() == 0;
    }
}
