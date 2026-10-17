package com.skillswap.matching;

import com.skillswap.dao.CategoryDao;
import com.skillswap.dao.SkillDao;
import com.skillswap.dao.jdbc.JdbcCategoryDao;
import com.skillswap.dao.jdbc.JdbcSkillDao;
import com.skillswap.model.Category;
import com.skillswap.model.Skill;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Thread-safe cache holding skill catalog indexed by category name in a TreeMap.
 */
public class CatalogCache {

    private static final Logger LOGGER = Logger.getLogger(CatalogCache.class.getName());
    private static final CatalogCache INSTANCE = new CatalogCache();

    private final ReadWriteLock rwLock = new ReentrantReadWriteLock();
    private final Lock readLock = rwLock.readLock();
    private final Lock writeLock = rwLock.writeLock();

    private final TreeMap<String, List<Skill>> cache = new TreeMap<>();
    private final List<Category> categoriesList = new ArrayList<>();

    private CatalogCache() {
    }

    /**
     * Returns the singleton instance of the catalog cache.
     *
     * @return catalog cache singleton
     */
    public static CatalogCache getInstance() {
        return INSTANCE;
    }

    /**
     * Reloads the cache from the database using DAOs under a write lock.
     *
     * @param categoryDao category DAO
     * @param skillDao skill DAO
     */
    public void reload(CategoryDao categoryDao, SkillDao skillDao) {
        writeLock.lock();
        try {
            cache.clear();
            categoriesList.clear();

            List<Category> allCategories = categoryDao.findAll();
            categoriesList.addAll(allCategories);

            for (Category category : allCategories) {
                List<Skill> skills = skillDao.findByCategory(category.getId());
                cache.put(category.getName(), new ArrayList<>(skills));
            }
            LOGGER.info("CatalogCache refreshed: " + categoriesList.size() + " categories loaded.");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to reload CatalogCache", e);
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * Convenience reload method using default JDBC DAOs.
     */
    public void reload() {
        reload(new JdbcCategoryDao(), new JdbcSkillDao());
    }

    /**
     * Returns an unmodifiable snapshot copy of category-to-skills mapping.
     *
     * @return copy of the cache mapping
     */
    public Map<String, List<Skill>> getSkillsByCategory() {
        readLock.lock();
        try {
            Map<String, List<Skill>> copy = new TreeMap<>();
            for (Map.Entry<String, List<Skill>> entry : cache.entrySet()) {
                copy.put(entry.getKey(), Collections.unmodifiableList(new ArrayList<>(entry.getValue())));
            }
            return Collections.unmodifiableMap(copy);
        } finally {
            readLock.unlock();
        }
    }

    /**
     * Returns the skills in a category, or empty list if category does not exist.
     *
     * @param categoryName category name
     * @return list of skills in category
     */
    public List<Skill> getSkillsForCategory(String categoryName) {
        readLock.lock();
        try {
            List<Skill> skills = cache.get(categoryName);
            if (skills == null) {
                return Collections.emptyList();
            }
            return Collections.unmodifiableList(new ArrayList<>(skills));
        } finally {
            readLock.unlock();
        }
    }

    /**
     * Returns an unmodifiable snapshot list of all categories.
     *
     * @return list of categories
     */
    public List<Category> getCategories() {
        readLock.lock();
        try {
            return Collections.unmodifiableList(new ArrayList<>(categoriesList));
        } finally {
            readLock.unlock();
        }
    }
}
