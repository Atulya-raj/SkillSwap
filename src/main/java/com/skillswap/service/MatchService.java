package com.skillswap.service;

import com.skillswap.exception.NoMatchFoundException;
import com.skillswap.matching.MatchEngine;
import com.skillswap.matching.MatchEngineFactory;
import com.skillswap.model.MatchResult;

import java.util.List;

/**
 * Service providing top matches and recommendations using the configured MatchEngine.
 */
public class MatchService {

    private static final MatchService INSTANCE = new MatchService();

    public MatchService() {
    }

    /**
     * Returns the singleton instance of MatchService.
     *
     * @return match service singleton
     */
    public static MatchService getInstance() {
        return INSTANCE;
    }

    /**
     * Finds top provider matches for a specific skill requested by a learner.
     *
     * @param learnerId learner user ID
     * @param skillId skill ID
     * @param topK maximum candidates
     * @return list of ranked match results
     * @throws NoMatchFoundException if no eligible providers exist
     */
    public List<MatchResult> findFor(long learnerId, long skillId, int topK) throws NoMatchFoundException {
        MatchEngine engine = MatchEngineFactory.getEngine();
        return engine.findProviders(learnerId, skillId, topK);
    }

    /**
     * Recommends top provider matches across all skills wanted by a learner.
     *
     * @param learnerId learner user ID
     * @param topK maximum recommendations
     * @return list of recommended match results
     */
    public List<MatchResult> recommend(long learnerId, int topK) {
        MatchEngine engine = MatchEngineFactory.getEngine();
        return engine.recommendFor(learnerId, topK);
    }
}
