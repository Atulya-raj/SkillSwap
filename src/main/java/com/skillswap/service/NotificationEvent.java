package com.skillswap.service;

import com.skillswap.model.enums.NotificationType;

/**
 * Event representing an asynchronous notification to be dispatched and stored.
 */
public class NotificationEvent {

    private final long userId;
    private final NotificationType type;
    private final String title;
    private final String body;
    private final String linkUrl;
    private final boolean poisonPill;

    /**
     * Constructs a standard notification event.
     *
     * @param userId recipient user ID
     * @param type notification category
     * @param title short summary title
     * @param body detailed notification message
     * @param linkUrl in-app route link
     */
    public NotificationEvent(long userId, NotificationType type, String title, String body, String linkUrl) {
        this.userId = userId;
        this.type = type;
        this.title = title;
        this.body = body;
        this.linkUrl = linkUrl;
        this.poisonPill = false;
    }

    private NotificationEvent(boolean poisonPill) {
        this.userId = 0;
        this.type = null;
        this.title = null;
        this.body = null;
        this.linkUrl = null;
        this.poisonPill = poisonPill;
    }

    /**
     * Factory method creating a poison pill event to signal graceful thread termination.
     *
     * @return poison pill event
     */
    public static NotificationEvent poisonPill() {
        return new NotificationEvent(true);
    }

    public long getUserId() {
        return userId;
    }

    public NotificationType getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public String getBody() {
        return body;
    }

    public String getLinkUrl() {
        return linkUrl;
    }

    public boolean isPoisonPill() {
        return poisonPill;
    }
}
