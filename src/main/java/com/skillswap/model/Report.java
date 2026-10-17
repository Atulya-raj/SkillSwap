package com.skillswap.model;

import com.skillswap.model.enums.ReportStatus;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Moderation report filed against a user profile or a review.
 */
public class Report {

    private long id;
    private long reporterId;
    private Long targetUserId;
    private Long targetReviewId;
    private String reason;
    private ReportStatus status = ReportStatus.OPEN;
    private LocalDateTime createdAt;
    private Long resolvedBy;
    private LocalDateTime resolvedAt;

    // Joined display fields
    private String reporterName;
    private String targetName;

    /**
     * Default constructor.
     */
    public Report() {
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getReporterId() {
        return reporterId;
    }

    public void setReporterId(long reporterId) {
        this.reporterId = reporterId;
    }

    public Long getTargetUserId() {
        return targetUserId;
    }

    public void setTargetUserId(Long targetUserId) {
        this.targetUserId = targetUserId;
    }

    public Long getTargetReviewId() {
        return targetReviewId;
    }

    public void setTargetReviewId(Long targetReviewId) {
        this.targetReviewId = targetReviewId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public ReportStatus getStatus() {
        return status;
    }

    public void setStatus(ReportStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Long getResolvedBy() {
        return resolvedBy;
    }

    public void setResolvedBy(Long resolvedBy) {
        this.resolvedBy = resolvedBy;
    }

    public LocalDateTime getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(LocalDateTime resolvedAt) {
        this.resolvedAt = resolvedAt;
    }

    public String getReporterName() {
        return reporterName;
    }

    public void setReporterName(String reporterName) {
        this.reporterName = reporterName;
    }

    public String getTargetName() {
        return targetName;
    }

    public void setTargetName(String targetName) {
        this.targetName = targetName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Report report = (Report) o;
        return id != 0 && id == report.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Report{id=" + id + ", reporterId=" + reporterId + ", status=" + status + "}";
    }
}
