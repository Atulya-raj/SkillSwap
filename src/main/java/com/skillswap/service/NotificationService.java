package com.skillswap.service;

import com.skillswap.dao.NotificationDao;
import com.skillswap.dao.jdbc.JdbcNotificationDao;
import com.skillswap.model.Notification;
import com.skillswap.model.enums.NotificationType;

import java.util.List;

/**
 * Service for publishing asynchronous notifications, querying unread counts, and listing history.
 */
public class NotificationService {

    private static volatile NotificationService instance;

    private final NotificationDispatcher dispatcher;
    private final NotificationDao notificationDao;

    /**
     * Default constructor using standard singleton dispatcher and JDBC DAO.
     */
    public NotificationService() {
        this(NotificationDispatcher.getInstance(), new JdbcNotificationDao());
    }

    /**
     * Constructs NotificationService with injected dependencies.
     *
     * @param dispatcher notification dispatcher
     * @param notificationDao DAO for notifications
     */
    public NotificationService(NotificationDispatcher dispatcher, NotificationDao notificationDao) {
        this.dispatcher = dispatcher;
        this.notificationDao = notificationDao;
    }

    /**
     * Returns singleton instance.
     *
     * @return global NotificationService instance
     */
    public static NotificationService getInstance() {
        if (instance == null) {
            synchronized (NotificationService.class) {
                if (instance == null) {
                    instance = new NotificationService();
                }
            }
        }
        return instance;
    }

    /**
     * Publishes an asynchronous notification to a user.
     *
     * @param userId recipient user ID
     * @param type notification category
     * @param title short notification title
     * @param body detailed notification message
     * @param linkUrl in-app route link
     */
    public void notify(long userId, NotificationType type, String title, String body, String linkUrl) {
        dispatcher.publish(new NotificationEvent(userId, type, title, body, linkUrl));
    }

    /**
     * Retrieves the count of unread notifications for a user.
     *
     * @param userId user ID
     * @return count of unread notifications
     */
    public int unreadCount(long userId) {
        return notificationDao.countUnread(userId);
    }

    /**
     * Retrieves total count of notifications for a user.
     *
     * @param userId user ID
     * @return total count of notifications
     */
    public int count(long userId) {
        return notificationDao.countByUser(userId);
    }

    /**
     * Retrieves paginated notifications for a user.
     *
     * @param userId user ID
     * @param limit maximum records
     * @param offset row offset
     * @return list of notifications
     */
    public List<Notification> list(long userId, int limit, int offset) {
        return notificationDao.findByUser(userId, limit, offset);
    }

    /**
     * Marks all notifications as read for a user.
     *
     * @param userId user ID
     */
    public void markAllRead(long userId) {
        notificationDao.markAllRead(userId);
    }

    /**
     * Marks a specific notification as read.
     *
     * @param id notification ID
     * @param userId recipient user ID
     */
    public void markRead(long id, long userId) {
        notificationDao.markRead(id, userId);
    }
}
