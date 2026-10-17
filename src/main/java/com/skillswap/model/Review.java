package com.skillswap.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Post-session peer review with rating and feedback comment.
 */
public class Review {

    private long id;
    private long sessionRequestId;
    private long reviewerId;
    private long revieweeId;
    private int rating;
    private String comment;
    private boolean hidden;
    private LocalDateTime createdAt;

    // Joined display fields
    private String reviewerName;

    /**
     * Default constructor.
     */
    public Review() {
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getSessionRequestId() {
        return sessionRequestId;
    }

    public void setSessionRequestId(long sessionRequestId) {
        this.sessionRequestId = sessionRequestId;
    }

    public long getReviewerId() {
        return reviewerId;
    }

    public void setReviewerId(long reviewerId) {
        this.reviewerId = reviewerId;
    }

    public long getRevieweeId() {
        return revieweeId;
    }

    public void setRevieweeId(long revieweeId) {
        this.revieweeId = revieweeId;
    }

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public boolean isHidden() {
        return hidden;
    }

    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getReviewerName() {
        return reviewerName;
    }

    public void setReviewerName(String reviewerName) {
        this.reviewerName = reviewerName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Review review = (Review) o;
        return id != 0 && id == review.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Review{id=" + id + ", sessionId=" + sessionRequestId + ", rating=" + rating + "}";
    }
}
