package com.skillswap.model;

import java.util.Objects;

/**
 * Teachable and learnable skill entity.
 */
public class Skill {

    private int id;
    private int categoryId;
    private String categoryName;
    private String name;
    private String description;

    /**
     * Default constructor.
     */
    public Skill() {
    }

    /**
     * Constructs a skill with category ID and name.
     *
     * @param categoryId category ID
     * @param name skill name
     */
    public Skill(int categoryId, String name) {
        this.categoryId = categoryId;
        this.name = name;
    }

    /**
     * Constructs a skill with id, category ID, and name.
     *
     * @param id skill ID
     * @param categoryId category ID
     * @param name skill name
     */
    public Skill(int id, int categoryId, String name) {
        this.id = id;
        this.categoryId = categoryId;
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(int categoryId) {
        this.categoryId = categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Skill skill = (Skill) o;
        return id != 0 && id == skill.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Skill{id=" + id + ", name='" + name + "', categoryId=" + categoryId + "}";
    }
}
