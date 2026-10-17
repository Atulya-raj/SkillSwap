package com.skillswap.model;

import com.skillswap.model.enums.SessionMode;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Serializable draft of a session request form saved to disk.
 */
public class RequestDraft implements Serializable {

    private static final long serialVersionUID = 1L;

    private long providerListingId;
    private String proposedStart;
    private int durationMinutes = 60;
    private SessionMode mode = SessionMode.ONLINE;
    private String meetingDetails;
    private String message;
    private LocalDateTime savedAt = LocalDateTime.now();

    /**
     * Default constructor.
     */
    public RequestDraft() {
    }

    public long getProviderListingId() {
        return providerListingId;
    }

    public void setProviderListingId(long providerListingId) {
        this.providerListingId = providerListingId;
    }

    public String getProposedStart() {
        return proposedStart;
    }

    public void setProposedStart(String proposedStart) {
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

    public LocalDateTime getSavedAt() {
        return savedAt;
    }

    public void setSavedAt(LocalDateTime savedAt) {
        this.savedAt = savedAt;
    }

    @Override
    public String toString() {
        return "RequestDraft{listingId=" + providerListingId + ", start='" + proposedStart + "'}";
    }
}
