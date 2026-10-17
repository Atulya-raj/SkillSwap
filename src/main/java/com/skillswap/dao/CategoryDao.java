package com.skillswap.dao;

import com.skillswap.model.Category;

import java.util.List;
import java.util.Optional;

/**
 * Data access interface for Category operations.
 */
public interface CategoryDao {

    List<Category> findAll();

    long insert(Category c);

    void update(Category c);

    void delete(int id);

    Optional<Category> findByName(String name);

    Optional<Category> findById(int id);
}
