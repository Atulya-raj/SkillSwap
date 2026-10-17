package com.skillswap.dao;

import com.skillswap.model.Notification;

import java.util.List;

/**
 * Data access interface for in-app user notifications.
 */
public interface NotificationDao {

    long insert(Notification n);

    List<Notification> findByUser(long userId, int limit, int offset);

    int countUnread(long userId);

    void markAllRead(long userId);

    void markRead(long id, long userId);
}
