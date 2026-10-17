package com.skillswap.service;

import com.skillswap.dao.NotificationDao;
import com.skillswap.dao.jdbc.JdbcNotificationDao;
import com.skillswap.model.Notification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Asynchronous notification dispatcher utilizing a daemon worker thread and poison-pill termination.
 */
public class NotificationDispatcher implements Runnable {

    private static final Logger LOGGER = Logger.getLogger(NotificationDispatcher.class.getName());
    private static volatile NotificationDispatcher instance;

    private final BlockingQueue<NotificationEvent> queue;
    private final NotificationDao notificationDao;
    private Thread workerThread;
    private volatile boolean running;

    /**
     * Default constructor wiring standard NotificationDao.
     */
    public NotificationDispatcher() {
        this(new JdbcNotificationDao(), new LinkedBlockingQueue<>());
    }

    /**
     * Constructs NotificationDispatcher with injected dependencies.
     *
     * @param notificationDao DAO for saving notifications
     * @param queue blocking queue for events
     */
    public NotificationDispatcher(NotificationDao notificationDao, BlockingQueue<NotificationEvent> queue) {
        this.notificationDao = notificationDao;
        this.queue = queue;
        this.running = false;
    }

    /**
     * Returns singleton instance.
     *
     * @return global NotificationDispatcher instance
     */
    public static NotificationDispatcher getInstance() {
        if (instance == null) {
            synchronized (NotificationDispatcher.class) {
                if (instance == null) {
                    instance = new NotificationDispatcher();
                }
            }
        }
        return instance;
    }

    /**
     * Resets singleton instance (useful for clean test environments).
     *
     * @param newInstance instance to set or null
     */
    public static synchronized void setInstance(NotificationDispatcher newInstance) {
        instance = newInstance;
    }

    /**
     * Starts the daemon worker thread.
     */
    public synchronized void start() {
        if (running) {
            return;
        }
        running = true;
        workerThread = new Thread(this, "skillswap-notification-dispatcher");
        workerThread.setDaemon(true);
        workerThread.start();
        LOGGER.log(Level.INFO, "NotificationDispatcher started as daemon thread.");
    }

    /**
     * Publishes a notification event to the asynchronous processing queue.
     *
     * @param event notification event
     */
    public void publish(NotificationEvent event) {
        if (event != null) {
            queue.offer(event);
        }
    }

    @Override
    public void run() {
        while (running) {
            try {
                NotificationEvent event = queue.take();
                if (event.isPoisonPill()) {
                    break;
                }
                processEvent(event);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Unexpected error in notification worker loop", e);
            }
        }

        // Drain any remaining items during graceful shutdown
        drainQueue();
        LOGGER.log(Level.INFO, "NotificationDispatcher terminated cleanly.");
    }

    private void processEvent(NotificationEvent event) {
        try {
            Notification n = new Notification();
            n.setUserId(event.getUserId());
            n.setType(event.getType());
            n.setTitle(event.getTitle());
            n.setBody(event.getBody());
            n.setLinkUrl(event.getLinkUrl());
            n.setRead(false);
            n.setCreatedAt(LocalDateTime.now());

            notificationDao.insert(n);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to persist notification for user " + event.getUserId() + ": " + e.getMessage(), e);
        }
    }

    /**
     * Drains all pending events from the queue and persists them immediately.
     */
    public void flush() {
        drainQueue();
    }

    private void drainQueue() {
        List<NotificationEvent> remaining = new ArrayList<>();
        queue.drainTo(remaining);
        for (NotificationEvent event : remaining) {
            if (!event.isPoisonPill()) {
                processEvent(event);
            }
        }
    }

    /**
     * Shuts down the dispatcher by submitting a poison pill and joining the worker thread.
     */
    public synchronized void stop() {
        if (!running) {
            return;
        }
        running = false;
        queue.offer(NotificationEvent.poisonPill());
        if (workerThread != null) {
            try {
                workerThread.join(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public boolean isRunning() {
        return running;
    }

    public int getQueueSize() {
        return queue.size();
    }
}
