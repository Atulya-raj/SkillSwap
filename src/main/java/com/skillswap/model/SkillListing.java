package com.skillswap.model;

import com.skillswap.exception.ValidationException;
import com.skillswap.model.enums.ListingType;
import com.skillswap.model.enums.SkillLevel;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Abstract base class for user skill listings (offered to teach or wanted to learn).
 */
public abstract class SkillListing {

    private long id;
    private long userId;
    private int skillId;
    private int categoryId;
    private String skillName;
    private String categoryName;
    private SkillLevel level;
    private String description;
    private boolean active = true;
    private LocalDateTime createdAt;

    /**
     * Default constructor.
     */
    public SkillListing() {
    }

    /**
     * Returns the listing discriminator type.
     *
     * @return ListingType OFFERED or WANTED
     */
    public abstract ListingType type();

    public ListingType getType() {
        return type();
    }

    /**
     * Returns a human-readable description of this listing.
     *
     * @return summary description
     */
    public abstract String describe();

    /**
     * Validates listing invariants, throwing ValidationException if invalid.
     *
     * @throws ValidationException if bounds or required fields are violated
     */
    public abstract void validate() throws ValidationException;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public int getSkillId() {
        return skillId;
    }

    public void setSkillId(int skillId) {
        this.skillId = skillId;
    }

    public int getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(int categoryId) {
        this.categoryId = categoryId;
    }

    public String getSkillName() {
        return skillName;
    }

    public void setSkillName(String skillName) {
        this.skillName = skillName;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public SkillLevel getLevel() {
        return level;
    }

    public void setLevel(SkillLevel level) {
        this.level = level;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SkillListing that = (SkillListing) o;
        return id != 0 && id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{id=" + id + ", userId=" + userId + ", skillId=" + skillId + "}";
    }
}
