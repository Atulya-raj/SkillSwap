package com.skillswap.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Achievement badge awarded for milestones and high reliability.
 */
public class Badge {

    private int id;
    private String code;
    private String name;
    private String description;
    private LocalDateTime awardedAt;

    /**
     * Default constructor.
     */
    public Badge() {
    }

    /**
     * Constructs a badge with basic attributes.
     *
     * @param id badge ID
     * @param code unique badge code
     * @param name badge title
     * @param description milestone criteria
     */
    public Badge(int id, String code, String name, String description) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.description = description;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
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

    public LocalDateTime getAwardedAt() {
        return awardedAt;
    }

    public void setAwardedAt(LocalDateTime awardedAt) {
        this.awardedAt = awardedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Badge badge = (Badge) o;
        return (id != 0 && id == badge.id) || (code != null && code.equals(badge.code));
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, code);
    }

    @Override
    public String toString() {
        return "Badge{code='" + code + "', name='" + name + "'}";
    }
}
