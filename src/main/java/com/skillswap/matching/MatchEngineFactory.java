package com.skillswap.matching;

import com.skillswap.util.Config;

/**
 * Factory creating configured MatchEngine implementation based on application properties.
 */
public final class MatchEngineFactory {

    private MatchEngineFactory() {
    }

    /**
     * Creates or returns the configured MatchEngine instance.
     *
     * @return match engine implementation
     */
    public static MatchEngine getEngine() {
        String engineType = Config.get("match.engine", "weighted");
        if ("rating".equalsIgnoreCase(engineType != null ? engineType.trim() : "")) {
            return new RatingFirstMatchEngine();
        }
        return new WeightedMatchEngine();
    }
}
