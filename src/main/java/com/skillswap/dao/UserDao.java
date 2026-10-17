package com.skillswap.dao;

import com.skillswap.model.User;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Data access interface for User operations.
 */
public interface UserDao {

    long insert(User u);

    Optional<User> findById(long id);

    Optional<User> findByEmail(String email);

    boolean emailExists(String email);

    void update(User u);

    void updatePassword(long id, String hash);

    void updateAvatar(long id, String path);

    void touchLogin(long id);

    void setActive(long id, boolean active);

    User lockForUpdate(long id);

    void setCredits(long id, int newBalance);

    void applyRatingStats(long id, BigDecimal avg, int count);

    void incrementTaught(long id);

    void incrementLearned(long id);

    void incrementStrikes(long id);

    List<User> search(String q, int limit, int offset);

    int count(String q);

    List<User> topMentorsByRating(int minReviews, int limit);

    List<User> topMentorsBySessions(int limit);

    List<User> findAll(int limit, int offset);
}
