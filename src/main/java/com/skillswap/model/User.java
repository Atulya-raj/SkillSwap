package com.skillswap.model;

import com.skillswap.model.enums.Role;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Abstract domain model representing an application user.
 */
public abstract class User implements Notifiable {

    private long id;
    private String fullName;
    private String email;
    private String passwordHash;
    private Role role = Role.STUDENT;
    private String department;
    private Integer studyYear;
    private String bio;
    private String avatarPath;
    private int credits;
    private BigDecimal avgRating = BigDecimal.ZERO;
    private int ratingCount;
    private int sessionsTaught;
    private int sessionsLearned;
    private int reliabilityStrikes;
    private boolean active = true;
    private LocalDateTime createdAt;
    private LocalDateTime lastLoginAt;

    /**
     * Default constructor.
     */
    public User() {
    }

    /**
     * Returns the default landing path for this user role after login.
     *
     * @return home URL path
     */
    public abstract String homePath();

    @Override
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @Override
    public String displayName() {
        return (fullName != null && !fullName.trim().isEmpty()) ? fullName : email;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public Integer getStudyYear() {
        return studyYear;
    }

    public void setStudyYear(Integer studyYear) {
        this.studyYear = studyYear;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getAvatarPath() {
        return avatarPath;
    }

    public void setAvatarPath(String avatarPath) {
        this.avatarPath = avatarPath;
    }

    public int getCredits() {
        return credits;
    }

    public void setCredits(int credits) {
        this.credits = credits;
    }

    public BigDecimal getAvgRating() {
        return avgRating;
    }

    public void setAvgRating(BigDecimal avgRating) {
        this.avgRating = avgRating;
    }

    public int getRatingCount() {
        return ratingCount;
    }

    public void setRatingCount(int ratingCount) {
        this.ratingCount = ratingCount;
    }

    public int getSessionsTaught() {
        return sessionsTaught;
    }

    public void setSessionsTaught(int sessionsTaught) {
        this.sessionsTaught = sessionsTaught;
    }

    public int getSessionsLearned() {
        return sessionsLearned;
    }

    public void setSessionsLearned(int sessionsLearned) {
        this.sessionsLearned = sessionsLearned;
    }

    public int getReliabilityStrikes() {
        return reliabilityStrikes;
    }

    public void setReliabilityStrikes(int reliabilityStrikes) {
        this.reliabilityStrikes = reliabilityStrikes;
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

    public LocalDateTime getLastLoginAt() {
        return lastLoginAt;
    }

    public void setLastLoginAt(LocalDateTime lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return id != 0 && id == user.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{id=" + id + ", email='" + email + "', role=" + role + "}";
    }
}
