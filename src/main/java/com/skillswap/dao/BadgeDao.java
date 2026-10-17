package com.skillswap.dao;

import com.skillswap.model.Badge;

import java.util.List;

/**
 * Data access interface for badges and user awards.
 */
public interface BadgeDao {

    List<Badge> findAll();

    List<Badge> findForUser(long userId);

    boolean has(long userId, String code);

    void award(long userId, String code);
}
