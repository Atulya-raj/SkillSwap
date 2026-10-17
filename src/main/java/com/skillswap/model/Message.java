package com.skillswap.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * In-app chat message exchanged within a session request thread.
 */
public class Message {

    private long id;
    private long sessionRequestId;
    private long senderId;
    private String body;
    private boolean read;
    private LocalDateTime createdAt;

    // Joined display fields
    private String senderName;

    /**
     * Default constructor.
     */
    public Message() {
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

    public long getSenderId() {
        return senderId;
    }

    public void setSenderId(long senderId) {
        this.senderId = senderId;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Message message = (Message) o;
        return id != 0 && id == message.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Message{id=" + id + ", sessionId=" + sessionRequestId + ", senderId=" + senderId + "}";
    }
}
