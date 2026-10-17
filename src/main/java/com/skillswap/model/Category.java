package com.skillswap.model;

import java.util.Objects;

/**
 * Skill category grouping related campus skills.
 */
public class Category {

    private int id;
    private String name;
    private String description;

    /**
     * Default constructor.
     */
    public Category() {
    }

    /**
     * Constructs a category with name and description.
     *
     * @param name category name
     * @param description category description
     */
    public Category(String name, String description) {
        this.name = name;
        this.description = description;
    }

    /**
     * Constructs a category with id, name, and description.
     *
     * @param id category ID
     * @param name category name
     * @param description category description
     */
    public Category(int id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
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
        Category category = (Category) o;
        return id != 0 && id == category.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Category{id=" + id + ", name='" + name + "'}";
    }
}
