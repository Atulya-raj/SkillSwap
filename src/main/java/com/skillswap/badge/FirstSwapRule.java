package com.skillswap.badge;

import com.skillswap.model.User;

/**
 * Awards FIRST_SWAP badge when user has completed at least one session as learner or teacher.
 */
public class FirstSwapRule implements BadgeRule {

    @Override
    public String code() {
        return "FIRST_SWAP";
    }

    @Override
    public boolean qualifies(User user, BadgeStats stats) {
        return stats.getTotalSessions() >= 1;
    }
}
