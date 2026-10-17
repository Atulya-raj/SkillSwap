package com.skillswap.badge;

import com.skillswap.model.User;

/**
 * Awards PERFECT_FIVE badge when user has received 5 or more 5-star ratings.
 */
public class PerfectFiveRule implements BadgeRule {

    @Override
    public String code() {
        return "PERFECT_FIVE";
    }

    @Override
    public boolean qualifies(User user, BadgeStats stats) {
        return stats.getFiveStarReviewsCount() >= 5;
    }
}
