package com.skillswap.service;

import com.skillswap.dao.CategoryDao;
import com.skillswap.dao.SkillDao;
import com.skillswap.dao.jdbc.JdbcCategoryDao;
import com.skillswap.dao.jdbc.JdbcSkillDao;
import com.skillswap.exception.ValidationException;
import com.skillswap.io.CsvImporter;
import com.skillswap.matching.CatalogCache;
import com.skillswap.model.Category;
import com.skillswap.model.Skill;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service managing skill and category catalog and cache interactions.
 */
public class CatalogService {

    private static final CatalogService INSTANCE = new CatalogService();

    private final CategoryDao categoryDao;
    private final SkillDao skillDao;
    private final CatalogCache catalogCache;

    public CatalogService() {
        this(new JdbcCategoryDao(), new JdbcSkillDao(), CatalogCache.getInstance());
    }

    public CatalogService(CategoryDao categoryDao, SkillDao skillDao, CatalogCache catalogCache) {
        this.categoryDao = categoryDao;
        this.skillDao = skillDao;
        this.catalogCache = catalogCache;
    }

    /**
     * Returns the singleton instance of the catalog service.
     *
     * @return catalog service singleton
     */
    public static CatalogService getInstance() {
        return INSTANCE;
    }

    /**
     * Returns all categories from the cache.
     *
     * @return list of categories
     */
    public List<Category> categories() {
        List<Category> list = catalogCache.getCategories();
        if (list.isEmpty()) {
            refreshCache();
            list = catalogCache.getCategories();
        }
        return list;
    }

    /**
     * Returns map of category names to skills list from the cache.
     *
     * @return map of skills by category name
     */
    public Map<String, List<Skill>> skillsByCategory() {
        Map<String, List<Skill>> map = catalogCache.getSkillsByCategory();
        if (map.isEmpty()) {
            refreshCache();
            map = catalogCache.getSkillsByCategory();
        }
        return map;
    }

    /**
     * Returns all skills across categories.
     *
     * @return list of skills
     */
    public List<Skill> allSkills() {
        return skillDao.findAll();
    }

    /**
     * Finds a skill by its unique identifier.
     *
     * @param id skill id
     * @return optional skill
     */
    public Optional<Skill> findSkillById(int id) {
        return skillDao.findById(id);
    }

    /**
     * Finds a category by its unique identifier.
     *
     * @param id category id
     * @return optional category
     */
    public Optional<Category> findCategoryById(int id) {
        return categoryDao.findById(id);
    }

    /**
     * Adds a new category and refreshes the cache.
     *
     * @param name category name
     * @param description category description
     * @return created category id
     * @throws ValidationException if category name is empty or invalid
     */
    public int createCategory(String name, String description) throws ValidationException {
        if (name == null || name.trim().isEmpty()) {
            Map<String, String> err = new HashMap<>();
            err.put("name", "Category name is required");
            throw new ValidationException(err);
        }
        Category cat = new Category();
        cat.setName(name.trim());
        cat.setDescription(description != null ? description.trim() : null);
        int id = (int) categoryDao.insert(cat);
        refreshCache();
        return id;
    }

    /**
     * Adds a new skill to a category and refreshes the cache.
     *
     * @param categoryId category id
     * @param name skill name
     * @param description skill description
     * @return created skill id
     * @throws ValidationException if name is invalid
     */
    public int createSkill(int categoryId, String name, String description) throws ValidationException {
        if (name == null || name.trim().isEmpty()) {
            Map<String, String> err = new HashMap<>();
            err.put("name", "Skill name is required");
            throw new ValidationException(err);
        }
        Skill skill = new Skill();
        skill.setCategoryId(categoryId);
        skill.setName(name.trim());
        skill.setDescription(description != null ? description.trim() : null);
        int id = (int) skillDao.insert(skill);
        refreshCache();
        return id;
    }

    /**
     * Updates an existing category's details and refreshes the cache.
     *
     * @param id category id
     * @param name category name
     * @param description category description
     * @throws ValidationException if name is invalid
     */
    public void updateCategory(int id, String name, String description) throws ValidationException {
        if (name == null || name.trim().isEmpty()) {
            Map<String, String> err = new HashMap<>();
            err.put("name", "Category name is required");
            throw new ValidationException(err);
        }
        Category cat = new Category();
        cat.setId(id);
        cat.setName(name.trim());
        cat.setDescription(description != null ? description.trim() : null);
        categoryDao.update(cat);
        refreshCache();
    }

    /**
     * Updates an existing skill's details and refreshes the cache.
     *
     * @param id skill id
     * @param categoryId category id
     * @param name skill name
     * @param description skill description
     * @throws ValidationException if name is invalid
     */
    public void updateSkill(int id, int categoryId, String name, String description) throws ValidationException {
        if (name == null || name.trim().isEmpty()) {
            Map<String, String> err = new HashMap<>();
            err.put("name", "Skill name is required");
            throw new ValidationException(err);
        }
        Skill skill = new Skill();
        skill.setId(id);
        skill.setCategoryId(categoryId);
        skill.setName(name.trim());
        skill.setDescription(description != null ? description.trim() : null);
        skillDao.update(skill);
        refreshCache();
    }

    /**
     * Deletes a category by id and refreshes cache.
     *
     * @param id category id
     */
    public void deleteCategory(int id) {
        categoryDao.delete(id);
        refreshCache();
    }

    /**
     * Deletes a skill by id and refreshes cache.
     *
     * @param id skill id
     */
    public void deleteSkill(int id) {
        skillDao.delete(id);
        refreshCache();
    }

    /**
     * Imports categories and skills from a CSV input stream using CsvImporter.
     *
     * @param is input stream containing CSV lines
     * @return ImportResult containing imported count and warnings
     */
    public CsvImporter.ImportResult importCatalogCsv(InputStream is) {
        try {
            CsvImporter importer = new CsvImporter();
            CsvImporter.ImportResult result = importer.importCatalog(is, categoryDao, skillDao);
            refreshCache();
            return result;
        } catch (Exception e) {
            throw new RuntimeException("Failed to import CSV", e);
        }
    }

    /**
     * Imports categories and skills from a CSV input stream (category,skill).
     *
     * @param is input stream containing CSV lines
     * @return number of imported skills
     */
    public int importCsv(InputStream is) {
        return importCatalogCsv(is).getImportedCount();
    }

    /**
     * Re-populates the in-memory catalog cache from database records.
     */
    public void refreshCache() {
        catalogCache.reload(categoryDao, skillDao);
    }
}
