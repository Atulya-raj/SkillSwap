package com.skillswap.scheduler;

import com.skillswap.dao.SessionRequestDao;
import com.skillswap.dao.jdbc.JdbcSessionRequestDao;
import com.skillswap.model.SessionRequest;
import com.skillswap.model.enums.NotificationType;
import com.skillswap.service.NotificationService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Scheduled background task sending reminders for upcoming accepted sessions starting within 60 minutes (F-27).
 */
public class ReminderTask implements Runnable {

    private static final Logger LOGGER = Logger.getLogger(ReminderTask.class.getName());

    private final SessionRequestDao sessionRequestDao;
    private final NotificationService notificationService;

    /**
     * Default constructor wiring standard implementations.
     */
    public ReminderTask() {
        this(new JdbcSessionRequestDao(), NotificationService.getInstance());
    }

    /**
     * Constructs ReminderTask with injected dependencies.
     *
     * @param sessionRequestDao DAO for session requests
     * @param notificationService service for dispatching notifications
     */
    public ReminderTask(SessionRequestDao sessionRequestDao, NotificationService notificationService) {
        this.sessionRequestDao = sessionRequestDao;
        this.notificationService = notificationService;
    }

    @Override
    public void run() {
        try {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime windowEnd = now.plusMinutes(60);
            List<SessionRequest> upcoming = sessionRequestDao.findAcceptedStartingBetween(now, windowEnd);

            for (SessionRequest sr : upcoming) {
                if (!sr.isReminderSent()) {
                    sessionRequestDao.markReminderSent(sr.getId());
                    sr.setReminderSent(true);

                    String skillName = sr.getSkillName() != null ? sr.getSkillName() : "SkillSwap Session";
                    String link = "/requests/view?id=" + sr.getId();

                    notificationService.notify(
                            sr.getLearnerId(),
                            NotificationType.SESSION_REMINDER,
                            "Upcoming Session Reminder",
                            "Your session for " + skillName + " starts in less than an hour! Mode: " + sr.getMode(),
                            link
                    );

                    notificationService.notify(
                            sr.getProviderId(),
                            NotificationType.SESSION_REMINDER,
                            "Upcoming Session Reminder",
                            "Your tutoring session for " + skillName + " starts in less than an hour! Mode: " + sr.getMode(),
                            link
                    );
                }
            }
        } catch (Throwable t) {
            LOGGER.log(Level.WARNING, "Error executing ReminderTask", t);
        }
    }
}
