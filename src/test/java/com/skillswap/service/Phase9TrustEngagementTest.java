package com.skillswap.service;

import com.skillswap.badge.BadgeRule;
import com.skillswap.badge.BadgeStats;
import com.skillswap.badge.ConnectorRule;
import com.skillswap.badge.EternalLearnerRule;
import com.skillswap.badge.FirstSwapRule;
import com.skillswap.badge.Mentor10Rule;
import com.skillswap.badge.PerfectFiveRule;
import com.skillswap.badge.ReliableRule;
import com.skillswap.dao.BadgeDao;
import com.skillswap.dao.ListingDao;
import com.skillswap.dao.MessageDao;
import com.skillswap.dao.NotificationDao;
import com.skillswap.dao.ReviewDao;
import com.skillswap.dao.SessionRequestDao;
import com.skillswap.dao.UserDao;
import com.skillswap.dao.jdbc.JdbcBadgeDao;
import com.skillswap.dao.jdbc.JdbcListingDao;
import com.skillswap.dao.jdbc.JdbcMessageDao;
import com.skillswap.dao.jdbc.JdbcNotificationDao;
import com.skillswap.dao.jdbc.JdbcReviewDao;
import com.skillswap.dao.jdbc.JdbcSessionRequestDao;
import com.skillswap.dao.jdbc.JdbcUserDao;
import com.skillswap.db.Database;
import com.skillswap.db.DatabaseInitializer;
import com.skillswap.exception.AuthorizationException;
import com.skillswap.exception.ValidationException;
import com.skillswap.model.Badge;
import com.skillswap.model.Message;
import com.skillswap.model.OfferedSkill;
import com.skillswap.model.Review;
import com.skillswap.model.SessionRequest;
import com.skillswap.model.Student;
import com.skillswap.model.User;
import com.skillswap.model.enums.Role;
import com.skillswap.model.enums.SessionMode;
import com.skillswap.model.enums.SessionStatus;
import com.skillswap.model.enums.SkillLevel;
import com.skillswap.util.Config;
import com.skillswap.util.PasswordHasher;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit and integration tests for Phase 9: Reviews, Badges, Messaging, and Leaderboard.
 */
public class Phase9TrustEngagementTest {

    private static UserDao userDao;
    private static ListingDao listingDao;
    private static SessionRequestDao sessionRequestDao;
    private static ReviewDao reviewDao;
    private static BadgeDao badgeDao;
    private static MessageDao messageDao;
    private static NotificationDao notificationDao;

    private static ReviewService reviewService;
    private static MessageService messageService;
    private static BadgeService badgeService;

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
        reviewDao = new JdbcReviewDao();
        badgeDao = new JdbcBadgeDao();
        messageDao = new JdbcMessageDao();
        notificationDao = new JdbcNotificationDao();

        badgeService = new BadgeService();
        reviewService = new ReviewService(reviewDao, sessionRequestDao, userDao, badgeService, NotificationService.getInstance());
        messageService = new MessageService(messageDao, sessionRequestDao, NotificationService.getInstance());
    }

    private Student createTestStudent(int initialCredits) {
        String token = UUID.randomUUID().toString().substring(0, 8);
        Student s = new Student();
        s.setFullName("Student " + token);
        s.setEmail("stud_" + token + "@campus.edu");
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

    private SessionRequest createCompletedSession(long learnerId, long providerId) {
        OfferedSkill off = new OfferedSkill();
        off.setUserId(providerId);
        off.setSkillId(1);
        off.setLevel(SkillLevel.INTERMEDIATE);
        off.setCreditsPerHour(2);
        off.setDescription("Test listing for reviews");
        off.setActive(true);
        long listingId = listingDao.insert(off);

        SessionRequest sr = new SessionRequest();
        sr.setLearnerId(learnerId);
        sr.setProviderId(providerId);
        sr.setSkillId(1);
        sr.setListingId(listingId);
        sr.setProposedStart(LocalDateTime.now().minusHours(2));
        sr.setDurationMinutes(60);
        sr.setCostCredits(2);
        sr.setMode(SessionMode.ONLINE);
        sr.setMeetingDetails("https://meet.google.com/test");
        sr.setStatus(SessionStatus.COMPLETED);
        sr.setLearnerConfirmed(true);
        sr.setProviderConfirmed(true);
        sr.setCreatedAt(LocalDateTime.now().minusDays(1));
        long reqId = sessionRequestDao.insert(sr);
        sr.setId(reqId);
        return sr;
    }

    @Test
    public void testReviewSubmissionAndStatsUpdate() throws Exception {
        Student learner = createTestStudent(10);
        Student mentor = createTestStudent(10);
        SessionRequest sr = createCompletedSession(learner.getId(), mentor.getId());

        // 1. Learner reviews mentor (5 stars)
        Review r1 = reviewService.submit(learner.getId(), sr.getId(), 5, "Brilliant mentor, very helpful!");
        assertNotNull(r1);
        assertEquals(5, r1.getRating());

        // Mentor's stats updated
        User updatedMentor = userDao.findById(mentor.getId()).orElseThrow();
        assertEquals(new BigDecimal("5.00"), updatedMentor.getAvgRating());
        assertEquals(1, updatedMentor.getRatingCount());

        // 2. Mentor reviews learner (4 stars)
        Review r2 = reviewService.submit(mentor.getId(), sr.getId(), 4, "Quick learner, had great questions.");
        assertNotNull(r2);
        assertEquals(4, r2.getRating());

        User updatedLearner = userDao.findById(learner.getId()).orElseThrow();
        assertEquals(new BigDecimal("4.00"), updatedLearner.getAvgRating());
        assertEquals(1, updatedLearner.getRatingCount());
    }

    @Test
    public void testDuplicateReviewRejected() throws Exception {
        Student learner = createTestStudent(10);
        Student mentor = createTestStudent(10);
        SessionRequest sr = createCompletedSession(learner.getId(), mentor.getId());

        reviewService.submit(learner.getId(), sr.getId(), 5, "First review");

        // Second review by learner should throw ValidationException
        assertThrows(ValidationException.class, () ->
                reviewService.submit(learner.getId(), sr.getId(), 5, "Duplicate review")
        );
    }

    @Test
    public void testNonParticipantCannotReview() {
        Student learner = createTestStudent(10);
        Student mentor = createTestStudent(10);
        Student outsider = createTestStudent(10);
        SessionRequest sr = createCompletedSession(learner.getId(), mentor.getId());

        assertThrows(AuthorizationException.class, () ->
                reviewService.submit(outsider.getId(), sr.getId(), 5, "I was not in this session")
        );
    }

    @Test
    public void testReviewNonCompletedSessionRejected() {
        Student learner = createTestStudent(10);
        Student mentor = createTestStudent(10);

        OfferedSkill off = new OfferedSkill();
        off.setUserId(mentor.getId());
        off.setSkillId(1);
        off.setLevel(SkillLevel.INTERMEDIATE);
        off.setCreditsPerHour(2);
        off.setDescription("Test listing for pending session");
        off.setActive(true);
        long listingId = listingDao.insert(off);

        SessionRequest sr = new SessionRequest();
        sr.setLearnerId(learner.getId());
        sr.setProviderId(mentor.getId());
        sr.setSkillId(1);
        sr.setListingId(listingId);
        sr.setProposedStart(LocalDateTime.now().plusDays(1));
        sr.setDurationMinutes(60);
        sr.setCostCredits(2);
        sr.setMode(SessionMode.ONLINE);
        sr.setStatus(SessionStatus.PENDING);
        long reqId = sessionRequestDao.insert(sr);

        assertThrows(ValidationException.class, () ->
                reviewService.submit(learner.getId(), reqId, 5, "Premature review")
        );
    }

    @Test
    public void testBadgeRuleBoundaries() {
        Student user = createTestStudent(10);

        // 1. FIRST_SWAP (sessionsTaught + sessionsLearned >= 1)
        BadgeRule firstSwap = new FirstSwapRule();
        assertFalse(firstSwap.qualifies(user, new BadgeStats(0, 0, BigDecimal.ZERO, 0, 0, 0)));
        assertTrue(firstSwap.qualifies(user, new BadgeStats(1, 0, BigDecimal.ZERO, 0, 0, 0)));
        assertTrue(firstSwap.qualifies(user, new BadgeStats(0, 1, BigDecimal.ZERO, 0, 0, 0)));

        // 2. MENTOR_10 (taught >= 10 && avgRating >= 4.5)
        BadgeRule mentor10 = new Mentor10Rule();
        assertFalse(mentor10.qualifies(user, new BadgeStats(9, 0, new BigDecimal("5.0"), 0, 0, 0)));
        assertFalse(mentor10.qualifies(user, new BadgeStats(10, 0, new BigDecimal("4.49"), 0, 0, 0)));
        assertTrue(mentor10.qualifies(user, new BadgeStats(10, 0, new BigDecimal("4.5"), 0, 0, 0)));
        assertTrue(mentor10.qualifies(user, new BadgeStats(12, 0, new BigDecimal("4.8"), 0, 0, 0)));

        // 3. ETERNAL_LEARNER (learned >= 10)
        BadgeRule eternalLearner = new EternalLearnerRule();
        assertFalse(eternalLearner.qualifies(user, new BadgeStats(0, 9, BigDecimal.ZERO, 0, 0, 0)));
        assertTrue(eternalLearner.qualifies(user, new BadgeStats(0, 10, BigDecimal.ZERO, 0, 0, 0)));

        // 4. PERFECT_FIVE (fiveStarReviews >= 5)
        BadgeRule perfectFive = new PerfectFiveRule();
        assertFalse(perfectFive.qualifies(user, new BadgeStats(0, 0, BigDecimal.ZERO, 4, 0, 0)));
        assertTrue(perfectFive.qualifies(user, new BadgeStats(0, 0, BigDecimal.ZERO, 5, 0, 0)));

        // 5. CONNECTOR (offeredSkills >= 3)
        BadgeRule connector = new ConnectorRule();
        assertFalse(connector.qualifies(user, new BadgeStats(0, 0, BigDecimal.ZERO, 0, 2, 0)));
        assertTrue(connector.qualifies(user, new BadgeStats(0, 0, BigDecimal.ZERO, 0, 3, 0)));

        // 6. RELIABLE (completed >= 5 && strikes == 0)
        BadgeRule reliable = new ReliableRule();
        assertFalse(reliable.qualifies(user, new BadgeStats(4, 0, BigDecimal.ZERO, 0, 0, 0)));
        assertFalse(reliable.qualifies(user, new BadgeStats(5, 0, BigDecimal.ZERO, 0, 0, 1)));
        assertTrue(reliable.qualifies(user, new BadgeStats(3, 2, BigDecimal.ZERO, 0, 0, 0)));

        // Award badge and verify idempotency
        badgeService.evaluate(user.getId());
        List<Badge> initialBadges = badgeDao.findForUser(user.getId());
        // Run evaluate again - no duplicate badges
        badgeService.evaluate(user.getId());
        List<Badge> secondBadges = badgeDao.findForUser(user.getId());
        assertEquals(initialBadges.size(), secondBadges.size());
    }

    @Test
    public void testMessagingThreadAndAuthorization() throws Exception {
        Student learner = createTestStudent(10);
        Student mentor = createTestStudent(10);
        Student outsider = createTestStudent(10);
        SessionRequest sr = createCompletedSession(learner.getId(), mentor.getId());

        // 1. Learner sends message to mentor
        Message m1 = messageService.send(learner.getId(), sr.getId(), "Hi mentor, looking forward to our session!");
        assertNotNull(m1);
        assertEquals(sr.getId(), m1.getSessionRequestId());
        assertEquals(learner.getId(), m1.getSenderId());
        assertFalse(m1.isRead());

        // Mentor has 1 unread message
        assertEquals(1, messageService.countUnread(mentor.getId()));
        // Learner has 0 unread messages (they are the sender)
        assertEquals(0, messageService.countUnread(learner.getId()));

        // 2. Mentor reads the thread
        List<Message> thread = messageService.thread(mentor.getId(), sr.getId());
        assertEquals(1, thread.size());
        assertEquals("Hi mentor, looking forward to our session!", thread.get(0).getBody());
        // After reading, unread count becomes 0
        assertEquals(0, messageService.countUnread(mentor.getId()));

        // 3. Outsider cannot read or post in thread
        assertThrows(AuthorizationException.class, () ->
                messageService.thread(outsider.getId(), sr.getId())
        );

        assertThrows(AuthorizationException.class, () ->
                messageService.send(outsider.getId(), sr.getId(), "Intruder message")
        );
    }

    @Test
    public void testLeaderboardQueries() {
        Student m1 = createTestStudent(10);
        Student m2 = createTestStudent(10);

        // Update stats
        userDao.applyRatingStats(m1.getId(), new BigDecimal("4.95"), 5);
        userDao.incrementTaught(m1.getId());
        userDao.incrementTaught(m1.getId());

        userDao.applyRatingStats(m2.getId(), new BigDecimal("4.80"), 4);
        userDao.incrementTaught(m2.getId());

        List<User> topByRating = userDao.topMentorsByRating(3, 10);
        assertNotNull(topByRating);
        assertTrue(topByRating.stream().anyMatch(u -> u.getId() == m1.getId()));
        assertTrue(topByRating.stream().anyMatch(u -> u.getId() == m2.getId()));

        List<User> topBySessions = userDao.topMentorsBySessions(10);
        assertNotNull(topBySessions);
        assertTrue(topBySessions.stream().anyMatch(u -> u.getId() == m1.getId()));
    }
}
