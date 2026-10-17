package com.skillswap.service;

import com.skillswap.dao.CreditDao;
import com.skillswap.dao.ListingDao;
import com.skillswap.dao.NotificationDao;
import com.skillswap.dao.SessionRequestDao;
import com.skillswap.dao.UserDao;
import com.skillswap.dao.jdbc.JdbcCreditDao;
import com.skillswap.dao.jdbc.JdbcListingDao;
import com.skillswap.dao.jdbc.JdbcNotificationDao;
import com.skillswap.dao.jdbc.JdbcSessionRequestDao;
import com.skillswap.dao.jdbc.JdbcUserDao;
import com.skillswap.db.Database;
import com.skillswap.db.DatabaseInitializer;
import com.skillswap.model.Notification;
import com.skillswap.model.OfferedSkill;
import com.skillswap.model.SessionRequest;
import com.skillswap.model.Student;
import com.skillswap.model.WantedSkill;
import com.skillswap.model.enums.NotificationType;
import com.skillswap.model.enums.Role;
import com.skillswap.model.enums.SessionMode;
import com.skillswap.model.enums.SessionStatus;
import com.skillswap.model.enums.SkillLevel;
import com.skillswap.scheduler.AutoCompleteTask;
import com.skillswap.scheduler.ExpiryTask;
import com.skillswap.scheduler.MatchScanTask;
import com.skillswap.scheduler.ReminderTask;
import com.skillswap.scheduler.SchedulerService;
import com.skillswap.util.Config;
import com.skillswap.util.PasswordHasher;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit and integration tests for background NotificationDispatcher, Scheduler tasks, and lifecycle.
 */
public class NotificationSchedulerTest {

    private static UserDao userDao;
    private static ListingDao listingDao;
    private static SessionRequestDao sessionRequestDao;
    private static NotificationDao notificationDao;
    private static CreditDao creditDao;
    private static CreditService creditService;
    private static SessionService sessionService;

    @BeforeAll
    public static void setUp() {
        Config.loadDefault();
        Config.setProperty("db.url", "jdbc:mysql://127.0.0.1:3306/skillswap_test?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Kolkata");
        Config.setProperty("db.user", "root");
        Config.setProperty("db.password", "change-me");
        Config.setProperty("db.autoinit", "true");
        Database.init();
        new DatabaseInitializer().initialize();

        userDao = new JdbcUserDao();
        listingDao = new JdbcListingDao();
        sessionRequestDao = new JdbcSessionRequestDao();
        notificationDao = new JdbcNotificationDao();
        creditDao = new JdbcCreditDao();
        creditService = new CreditService(userDao, creditDao);
        sessionService = new SessionService(
                sessionRequestDao,
                listingDao,
                userDao,
                creditService,
                new BadgeService(),
                new DraftService()
        );
    }

    private Student createTestStudent(int initialCredits) {
        String token = UUID.randomUUID().toString().substring(0, 8);
        Student s = new Student();
        s.setFullName("Test User " + token);
        s.setEmail("user_" + token + "@campus.edu");
        s.setPasswordHash(PasswordHasher.hash("Password123"));
        s.setRole(Role.STUDENT);
        s.setActive(true);
        s.setCredits(initialCredits);
        s.setDepartment("Computer Science");
        s.setStudyYear(2);
        long id = userDao.insert(s);
        s.setId(id);
        return s;
    }

    @Test
    public void testDispatcherAsyncDelivery() throws Exception {
        Student user = createTestStudent(10);
        NotificationDispatcher dispatcher = new NotificationDispatcher(notificationDao, new LinkedBlockingQueue<>());
        dispatcher.start();

        try {
            NotificationEvent event = new NotificationEvent(
                    user.getId(),
                    NotificationType.ADMIN_NOTICE,
                    "Welcome Test",
                    "Async delivery test message",
                    "/profile"
            );
            dispatcher.publish(event);

            // Wait for worker thread to process
            boolean found = false;
            for (int i = 0; i < 20; i++) {
                Thread.sleep(100);
                List<Notification> list = notificationDao.findByUser(user.getId(), 10, 0);
                if (!list.isEmpty()) {
                    found = true;
                    Notification n = list.get(0);
                    assertEquals("Welcome Test", n.getTitle());
                    assertEquals("Async delivery test message", n.getBody());
                    assertEquals(NotificationType.ADMIN_NOTICE, n.getType());
                    assertFalse(n.isRead());
                    break;
                }
            }
            assertTrue(found, "Notification should be persisted asynchronously by worker thread");
        } finally {
            dispatcher.stop();
        }
    }

    @Test
    public void testDispatcherQueueDrainOnShutdown() {
        Student user = createTestStudent(10);
        LinkedBlockingQueue<NotificationEvent> queue = new LinkedBlockingQueue<>();
        NotificationDispatcher dispatcher = new NotificationDispatcher(notificationDao, queue);

        // Offer 3 events before starting
        queue.offer(new NotificationEvent(user.getId(), NotificationType.ADMIN_NOTICE, "Event 1", "Body 1", null));
        queue.offer(new NotificationEvent(user.getId(), NotificationType.ADMIN_NOTICE, "Event 2", "Body 2", null));
        queue.offer(new NotificationEvent(user.getId(), NotificationType.ADMIN_NOTICE, "Event 3", "Body 3", null));

        dispatcher.start();
        // Immediately stop - queue drain must process all events
        dispatcher.stop();

        List<Notification> list = notificationDao.findByUser(user.getId(), 10, 0);
        assertEquals(3, list.size(), "Dispatcher shutdown must drain remaining queue items");
    }

    @Test
    public void testMatchScanTaskAlertsLearnerAndIsIdempotent() {
        Student learner = createTestStudent(10);
        Student mentor = createTestStudent(10);

        // Learner wants skill ID 1 (Java Programming)
        WantedSkill wanted = new WantedSkill();
        wanted.setUserId(learner.getId());
        wanted.setSkillId(1);
        wanted.setLevel(SkillLevel.BEGINNER);
        wanted.setActive(true);
        listingDao.insert(wanted);

        // Mentor offers skill ID 1
        OfferedSkill offered = new OfferedSkill();
        offered.setUserId(mentor.getId());
        offered.setSkillId(1);
        offered.setLevel(SkillLevel.ADVANCED);
        offered.setCreditsPerHour(2);
        offered.setDescription("Teaching core Java");
        offered.setActive(true);
        listingDao.insert(offered);

        MatchScanTask task = new MatchScanTask();
        // Run scan task
        task.run();
        NotificationDispatcher.getInstance().flush();

        // Check if learner received notification
        List<Notification> notifs = notificationDao.findByUser(learner.getId(), 10, 0);
        boolean receivedMatch = notifs.stream().anyMatch(n -> n.getType() == NotificationType.NEW_MATCH);
        assertTrue(receivedMatch, "Learner should receive NEW_MATCH notification for newly offered skill");

        int countBefore = notifs.size();
        // Run scan task again immediately - should be idempotent
        task.run();
        NotificationDispatcher.getInstance().flush();
        List<Notification> notifsAfter = notificationDao.findByUser(learner.getId(), 10, 0);
        assertEquals(countBefore, notifsAfter.size(), "Second scan with no new listings must not produce duplicate notifications");
    }

    @Test
    public void testReminderTaskAlertsBothParticipantsAndIsIdempotent() {
        Student learner = createTestStudent(10);
        Student mentor = createTestStudent(10);

        OfferedSkill offered = new OfferedSkill();
        offered.setUserId(mentor.getId());
        offered.setSkillId(1);
        offered.setLevel(SkillLevel.INTERMEDIATE);
        offered.setCreditsPerHour(2);
        offered.setDescription("Java reminders");
        offered.setActive(true);
        long offeredId = listingDao.insert(offered);

        // Create accepted session starting in 30 minutes
        LocalDateTime startTime = LocalDateTime.now().plusMinutes(30);
        SessionRequest req = new SessionRequest();
        req.setLearnerId(learner.getId());
        req.setProviderId(mentor.getId());
        req.setSkillId(1);
        req.setListingId(offeredId);
        req.setProposedStart(startTime);
        req.setDurationMinutes(60);
        req.setCostCredits(2);
        req.setMode(SessionMode.ONLINE);
        req.setMeetingDetails("https://meet.skillswap.local/room-123");
        req.setStatus(SessionStatus.ACCEPTED);
        req.setReminderSent(false);
        req.setCreatedAt(LocalDateTime.now().minusHours(1));
        long reqId = sessionRequestDao.insert(req);

        ReminderTask reminderTask = new ReminderTask();
        reminderTask.run();
        NotificationDispatcher.getInstance().flush();

        // Verify reminder_sent is true
        SessionRequest updated = sessionRequestDao.findById(reqId).orElseThrow();
        assertTrue(updated.isReminderSent(), "SessionRequest.reminderSent must be set to true");

        // Verify notifications sent to both learner and mentor
        List<Notification> learnerNotifs = notificationDao.findByUser(learner.getId(), 10, 0);
        List<Notification> mentorNotifs = notificationDao.findByUser(mentor.getId(), 10, 0);

        assertTrue(learnerNotifs.stream().anyMatch(n -> n.getType() == NotificationType.SESSION_REMINDER),
                "Learner should receive session reminder");
        assertTrue(mentorNotifs.stream().anyMatch(n -> n.getType() == NotificationType.SESSION_REMINDER),
                "Mentor should receive session reminder");

        // Second run must be idempotent
        int learnerCount = learnerNotifs.size();
        int mentorCount = mentorNotifs.size();
        reminderTask.run();
        NotificationDispatcher.getInstance().flush();

        assertEquals(learnerCount, notificationDao.findByUser(learner.getId(), 10, 0).size(),
                "Reminder task must be idempotent");
        assertEquals(mentorCount, notificationDao.findByUser(mentor.getId(), 10, 0).size(),
                "Reminder task must be idempotent");
    }

    @Test
    public void testExpiryTaskExpiresStalePendingRequests() throws Exception {
        Student learner = createTestStudent(10);
        Student mentor = createTestStudent(10);

        OfferedSkill offered = new OfferedSkill();
        offered.setUserId(mentor.getId());
        offered.setSkillId(1);
        offered.setLevel(SkillLevel.INTERMEDIATE);
        offered.setCreditsPerHour(2);
        offered.setDescription("Java mentorship");
        offered.setActive(true);
        long offeredId = listingDao.insert(offered);

        // Create session via SessionService (holds 2 credits)
        SessionRequest req = sessionService.create(
                learner.getId(),
                offeredId,
                LocalDateTime.now().plusDays(2),
                60,
                SessionMode.ONLINE,
                "https://meet.example.com",
                "Hi!"
        );

        // Verify learner credits held
        assertEquals(8, userDao.findById(learner.getId()).orElseThrow().getCredits());
        assertEquals(2, creditService.getHeldCredits(learner.getId()));

        // Backdate created_at to 72 hours ago
        backdateSessionRequestCreatedAt(req.getId(), LocalDateTime.now().minusHours(72));

        ExpiryTask expiryTask = new ExpiryTask();
        expiryTask.run();

        // Verify status is EXPIRED
        SessionRequest expired = sessionRequestDao.findById(req.getId()).orElseThrow();
        assertEquals(SessionStatus.EXPIRED, expired.getStatus());

        // Verify held credits released back to learner
        assertEquals(10, userDao.findById(learner.getId()).orElseThrow().getCredits());
        assertEquals(0, creditService.getHeldCredits(learner.getId()));

        // Run again to verify idempotency
        expiryTask.run();
        assertEquals(SessionStatus.EXPIRED, sessionRequestDao.findById(req.getId()).orElseThrow().getStatus());
        assertEquals(10, userDao.findById(learner.getId()).orElseThrow().getCredits());
    }

    private void backdateSessionRequestCreatedAt(long requestId, LocalDateTime createdAt) {
        String sql = "UPDATE session_requests SET created_at = ? WHERE id = ?";
        try (Connection con = Database.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setTimestamp(1, java.sql.Timestamp.valueOf(createdAt));
            ps.setLong(2, requestId);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testSchedulerServiceLifecycle() {
        SchedulerService scheduler = new SchedulerService();
        assertFalse(scheduler.isStarted());

        scheduler.start();
        assertTrue(scheduler.isStarted());

        scheduler.stop();
        assertFalse(scheduler.isStarted());
    }
}
