package com.skillswap.badge;

import com.skillswap.model.User;

import java.math.BigDecimal;

/**
 * Awards MENTOR_10 badge when user has taught 10 sessions and has an average rating >= 4.5.
 */
public class Mentor10Rule implements BadgeRule {

    private static final BigDecimal MIN_RATING = new BigDecimal("4.5");

    @Override
    public String code() {
        return "MENTOR_10";
    }

    @Override
    public boolean qualifies(User user, BadgeStats stats) {
        return stats.getSessionsTaught() >= 10 && stats.getAvgRating().compareTo(MIN_RATING) >= 0;
    }
}
