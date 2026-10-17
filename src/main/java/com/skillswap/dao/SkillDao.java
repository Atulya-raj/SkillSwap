package com.skillswap.dao;

import com.skillswap.model.Skill;

import java.util.List;
import java.util.Optional;

/**
 * Data access interface for Skill operations.
 */
public interface SkillDao {

    List<Skill> findAll();

    List<Skill> findByCategory(int categoryId);

    long insert(Skill s);

    void update(Skill s);

    void delete(int id);

    Optional<Skill> findByCategoryAndName(int categoryId, String name);

    Optional<Skill> findById(int id);
}
