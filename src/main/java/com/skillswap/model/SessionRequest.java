package com.skillswap.model;

import com.skillswap.model.enums.SessionMode;
import com.skillswap.model.enums.SessionStatus;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Domain model representing a peer-to-peer skill swap session request.
 */
public class SessionRequest {

    private long id;
    private long learnerId;
    private long providerId;
    private int skillId;
    private long listingId;
    private LocalDateTime proposedStart;
    private int durationMinutes = 60;
    private SessionMode mode = SessionMode.ONLINE;
    private String meetingDetails;
    private String message;
    private int costCredits;
    private SessionStatus status = SessionStatus.PENDING;
    private boolean learnerConfirmed;
    private boolean providerConfirmed;
    private boolean reminderSent;
    private LocalDateTime createdAt;
    private LocalDateTime respondedAt;
    private LocalDateTime firstConfirmedAt;
    private LocalDateTime completedAt;

    // Joined display fields
    private String learnerName;
    private String providerName;
    private String skillName;
    private String learnerAvatar;
    private String providerAvatar;

    /**
     * Default constructor.
     */
    public SessionRequest() {
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getLearnerId() {
        return learnerId;
    }

    public void setLearnerId(long learnerId) {
        this.learnerId = learnerId;
    }

    public long getProviderId() {
        return providerId;
    }

    public void setProviderId(long providerId) {
        this.providerId = providerId;
    }

    public int getSkillId() {
        return skillId;
    }

    public void setSkillId(int skillId) {
        this.skillId = skillId;
    }

    public long getListingId() {
        return listingId;
    }

    public void setListingId(long listingId) {
        this.listingId = listingId;
    }

    public LocalDateTime getProposedStart() {
        return proposedStart;
    }

    public void setProposedStart(LocalDateTime proposedStart) {
        this.proposedStart = proposedStart;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(int durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public SessionMode getMode() {
        return mode;
    }

    public void setMode(SessionMode mode) {
        this.mode = mode;
    }

    public String getMeetingDetails() {
        return meetingDetails;
    }

    public void setMeetingDetails(String meetingDetails) {
        this.meetingDetails = meetingDetails;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getCostCredits() {
        return costCredits;
    }

    public void setCostCredits(int costCredits) {
        this.costCredits = costCredits;
    }

    public SessionStatus getStatus() {
        return status;
    }

    public void setStatus(SessionStatus status) {
        this.status = status;
    }

    public boolean isLearnerConfirmed() {
        return learnerConfirmed;
    }

    public void setLearnerConfirmed(boolean learnerConfirmed) {
        this.learnerConfirmed = learnerConfirmed;
    }

    public boolean isProviderConfirmed() {
        return providerConfirmed;
    }

    public void setProviderConfirmed(boolean providerConfirmed) {
        this.providerConfirmed = providerConfirmed;
    }

    public boolean isReminderSent() {
        return reminderSent;
    }

    public void setReminderSent(boolean reminderSent) {
        this.reminderSent = reminderSent;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getRespondedAt() {
        return respondedAt;
    }

    public void setRespondedAt(LocalDateTime respondedAt) {
        this.respondedAt = respondedAt;
    }

    public LocalDateTime getFirstConfirmedAt() {
        return firstConfirmedAt;
    }

    public void setFirstConfirmedAt(LocalDateTime firstConfirmedAt) {
        this.firstConfirmedAt = firstConfirmedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public String getLearnerName() {
        return learnerName;
    }

    public void setLearnerName(String learnerName) {
        this.learnerName = learnerName;
    }

    public String getProviderName() {
        return providerName;
    }

    public void setProviderName(String providerName) {
        this.providerName = providerName;
    }

    public String getSkillName() {
        return skillName;
    }

    public void setSkillName(String skillName) {
        this.skillName = skillName;
    }

    public String getLearnerAvatar() {
        return learnerAvatar;
    }

    public void setLearnerAvatar(String learnerAvatar) {
        this.learnerAvatar = learnerAvatar;
    }

    public String getProviderAvatar() {
        return providerAvatar;
    }

    public void setProviderAvatar(String providerAvatar) {
        this.providerAvatar = providerAvatar;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SessionRequest that = (SessionRequest) o;
        return id != 0 && id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "SessionRequest{id=" + id + ", learnerId=" + learnerId + ", providerId=" + providerId + ", status=" + status + "}";
    }
}
