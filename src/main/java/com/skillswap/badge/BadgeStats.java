package com.skillswap.badge;

import java.math.BigDecimal;

/**
 * Value object holding user statistics used to evaluate badge eligibility.
 */
public class BadgeStats {

    private final int sessionsTaught;
    private final int sessionsLearned;
    private final BigDecimal avgRating;
    private final int fiveStarReviewsCount;
    private final int offeredSkillsCount;
    private final int reliabilityStrikes;

    public BadgeStats(int sessionsTaught, int sessionsLearned, BigDecimal avgRating,
                      int fiveStarReviewsCount, int offeredSkillsCount, int reliabilityStrikes) {
        this.sessionsTaught = sessionsTaught;
        this.sessionsLearned = sessionsLearned;
        this.avgRating = avgRating != null ? avgRating : BigDecimal.ZERO;
        this.fiveStarReviewsCount = fiveStarReviewsCount;
        this.offeredSkillsCount = offeredSkillsCount;
        this.reliabilityStrikes = reliabilityStrikes;
    }

    public int getSessionsTaught() {
        return sessionsTaught;
    }

    public int getSessionsLearned() {
        return sessionsLearned;
    }

    public int getTotalSessions() {
        return sessionsTaught + sessionsLearned;
    }

    public BigDecimal getAvgRating() {
        return avgRating;
    }

    public int getFiveStarReviewsCount() {
        return fiveStarReviewsCount;
    }

    public int getOfferedSkillsCount() {
        return offeredSkillsCount;
    }

    public int getReliabilityStrikes() {
        return reliabilityStrikes;
    }
}
