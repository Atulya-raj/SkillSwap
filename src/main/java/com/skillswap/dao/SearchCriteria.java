package com.skillswap.dao;

import com.skillswap.model.enums.SkillLevel;

import java.math.BigDecimal;
import java.time.DayOfWeek;

/**
 * Filter criteria for searching and browsing offered skill listings.
 */
public class SearchCriteria {

    private String keyword;
    private Integer categoryId;
    private Integer skillId;
    private SkillLevel level;
    private BigDecimal minRating;
    private DayOfWeek dayOfWeek;
    private String sort = "BEST"; // BEST, RATING, PRICE

    /**
     * Default constructor.
     */
    public SearchCriteria() {
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public Integer getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Integer categoryId) {
        this.categoryId = categoryId;
    }

    public Integer getSkillId() {
        return skillId;
    }

    public void setSkillId(Integer skillId) {
        this.skillId = skillId;
    }

    public SkillLevel getLevel() {
        return level;
    }

    public void setLevel(SkillLevel level) {
        this.level = level;
    }

    public BigDecimal getMinRating() {
        return minRating;
    }

    public void setMinRating(BigDecimal minRating) {
        this.minRating = minRating;
    }

    public DayOfWeek getDayOfWeek() {
        return dayOfWeek;
    }

    public void setDayOfWeek(DayOfWeek dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    public String getSort() {
        return sort;
    }

    public void setSort(String sort) {
        this.sort = sort;
    }
}
