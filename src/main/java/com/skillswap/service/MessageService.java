package com.skillswap.service;

import com.skillswap.dao.MessageDao;
import com.skillswap.dao.SessionRequestDao;
import com.skillswap.dao.jdbc.JdbcMessageDao;
import com.skillswap.dao.jdbc.JdbcSessionRequestDao;
import com.skillswap.exception.AuthorizationException;
import com.skillswap.exception.ResourceNotFoundException;
import com.skillswap.exception.ValidationException;
import com.skillswap.model.Message;
import com.skillswap.model.SessionRequest;
import com.skillswap.model.enums.NotificationType;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service for managing session chat threads, participant authorization, and unread counts.
 */
public class MessageService {

    private static volatile MessageService instance;

    private final MessageDao messageDao;
    private final SessionRequestDao sessionRequestDao;
    private final NotificationService notificationService;

    /**
     * Default constructor wiring standard DAOs and NotificationService.
     */
    public MessageService() {
        this(new JdbcMessageDao(), new JdbcSessionRequestDao(), NotificationService.getInstance());
    }

    /**
     * Constructs MessageService with injected dependencies.
     *
     * @param messageDao message DAO
     * @param sessionRequestDao session request DAO
     * @param notificationService notification service
     */
    public MessageService(MessageDao messageDao, SessionRequestDao sessionRequestDao,
                          NotificationService notificationService) {
        this.messageDao = messageDao;
        this.sessionRequestDao = sessionRequestDao;
        this.notificationService = notificationService;
    }

    /**
     * Returns singleton instance.
     *
     * @return global MessageService instance
     */
    public static MessageService getInstance() {
        if (instance == null) {
            synchronized (MessageService.class) {
                if (instance == null) {
                    instance = new MessageService();
                }
            }
        }
        return instance;
    }

    /**
     * Sends a message in a session thread, enforcing participant authorization.
     *
     * @param senderId user sending the message
     * @param sessionId session request ID
     * @param body message body
     * @return persisted Message
     * @throws Exception on validation or authorization error
     */
    public Message send(long senderId, long sessionId, String body) throws Exception {
        if (body == null || body.trim().isEmpty()) {
            throw new ValidationException("body", "Message body cannot be empty.");
        }
        if (body.trim().length() > 2000) {
            throw new ValidationException("body", "Message cannot exceed 2000 characters.");
        }

        SessionRequest sr = sessionRequestDao.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session request not found: " + sessionId));

        if (sr.getLearnerId() != senderId && sr.getProviderId() != senderId) {
            throw new AuthorizationException("Access denied: You are not a participant in this session thread.");
        }

        long recipientId = (senderId == sr.getLearnerId()) ? sr.getProviderId() : sr.getLearnerId();

        Message m = new Message();
        m.setSessionRequestId(sessionId);
        m.setSenderId(senderId);
        m.setBody(body.trim());
        m.setRead(false);
        m.setCreatedAt(LocalDateTime.now());

        long id = messageDao.insert(m);
        m.setId(id);

        if (notificationService != null) {
            String skillInfo = sr.getSkillName() != null ? sr.getSkillName() : ("session #" + sessionId);
            notificationService.notify(
                    recipientId,
                    NotificationType.NEW_MESSAGE,
                    "New Chat Message",
                    "You have a new message regarding " + skillInfo + ".",
                    "/messages?requestId=" + sessionId
            );
        }

        return m;
    }

    /**
     * Retrieves the chronological message thread for a session, marking unread messages as read.
     *
     * @param userId user requesting the thread
     * @param sessionId session request ID
     * @return list of messages
     * @throws Exception on not found or authorization failure
     */
    public List<Message> thread(long userId, long sessionId) throws Exception {
        SessionRequest sr = sessionRequestDao.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session request not found: " + sessionId));

        if (sr.getLearnerId() != userId && sr.getProviderId() != userId) {
            throw new AuthorizationException("Access denied: You are not a participant in this session thread.");
        }

        messageDao.markThreadRead(sessionId, userId);
        return messageDao.findThread(sessionId);
    }

    /**
     * Counts unread messages for a user across all their session threads.
     *
     * @param userId user ID
     * @return unread messages count
     */
    public int countUnread(long userId) {
        return messageDao.countUnread(userId);
    }
}
