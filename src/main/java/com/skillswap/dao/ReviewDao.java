package com.skillswap.dao;

import com.skillswap.model.Review;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Data access interface for post-session reviews.
 */
public interface ReviewDao {

    long insert(Review r);

    boolean exists(long sessionId, long reviewerId);

    List<Review> findForUser(long userId, int limit);

    BigDecimal avgFor(long userId);

    int countFor(long userId);

    void setHidden(long id, boolean hidden);

    Optional<Review> findById(long id);

    int countFiveStar(long userId);
}
