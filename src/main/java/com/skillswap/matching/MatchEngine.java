package com.skillswap.matching;

import com.skillswap.exception.NoMatchFoundException;
import com.skillswap.model.MatchResult;

import java.util.List;

/**
 * Strategy interface for matching learners with suitable skill providers.
 */
public interface MatchEngine {

    /**
     * Finds and ranks provider candidates for a specific skill.
     *
     * @param learnerId learner user ID
     * @param skillId requested skill ID
     * @param topK maximum number of matches to return
     * @return ranked list of match results
     * @throws NoMatchFoundException if no eligible providers exist
     */
    List<MatchResult> findProviders(long learnerId, long skillId, int topK) throws NoMatchFoundException;

    /**
     * Recommends top provider matches across all of a learner's wanted skills.
     *
     * @param learnerId learner user ID
     * @param topK maximum number of recommendations
     * @return list of recommended match results
     */
    List<MatchResult> recommendFor(long learnerId, int topK);
}
