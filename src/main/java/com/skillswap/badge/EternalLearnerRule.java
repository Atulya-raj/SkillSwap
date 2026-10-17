package com.skillswap.badge;

import com.skillswap.model.User;

/**
 * Awards ETERNAL_LEARNER badge when user has participated as a learner in at least 10 completed sessions.
 */
public class EternalLearnerRule implements BadgeRule {

    @Override
    public String code() {
        return "ETERNAL_LEARNER";
    }

    @Override
    public boolean qualifies(User user, BadgeStats stats) {
        return stats.getSessionsLearned() >= 10;
    }
}
