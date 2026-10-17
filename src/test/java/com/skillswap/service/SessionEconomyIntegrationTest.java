package com.skillswap.service;

import com.skillswap.dao.CreditDao;
import com.skillswap.dao.ListingDao;
import com.skillswap.dao.SessionRequestDao;
import com.skillswap.dao.UserDao;
import com.skillswap.dao.jdbc.JdbcCreditDao;
import com.skillswap.dao.jdbc.JdbcListingDao;
import com.skillswap.dao.jdbc.JdbcSessionRequestDao;
import com.skillswap.dao.jdbc.JdbcUserDao;
import com.skillswap.db.Database;
import com.skillswap.db.DatabaseInitializer;
import com.skillswap.exception.InsufficientCreditsException;
import com.skillswap.exception.InvalidStateTransitionException;
import com.skillswap.exception.SkillSwapException;
import com.skillswap.model.CreditEntry;
import com.skillswap.model.OfferedSkill;
import com.skillswap.model.RequestDraft;
import com.skillswap.model.SessionRequest;
import com.skillswap.model.Student;
import com.skillswap.model.User;
import com.skillswap.model.enums.LedgerType;
import com.skillswap.model.enums.Role;
import com.skillswap.model.enums.SessionMode;
import com.skillswap.model.enums.SessionStatus;
import com.skillswap.model.enums.SkillLevel;
import com.skillswap.util.Config;
import com.skillswap.util.PasswordHasher;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration and multithreaded concurrency tests for SessionService and CreditService.
 */
public class SessionEconomyIntegrationTest {

    private static SessionService sessionService;
    private static CreditService creditService;
    private static DraftService draftService;
    private static UserDao userDao;
    private static ListingDao listingDao;
    private static SessionRequestDao sessionRequestDao;
    private static CreditDao creditDao;

    @BeforeAll
    public static void setUp() {
        Config.loadDefault();
        Database.init();
        new DatabaseInitializer().initialize();

        userDao = new JdbcUserDao();
        listingDao = new JdbcListingDao();
        sessionRequestDao = new JdbcSessionRequestDao();
        creditDao = new JdbcCreditDao();

        creditService = new CreditService(userDao, creditDao);
        draftService = new DraftService();
        BadgeService badgeService = new BadgeService();
        sessionService = new SessionService(sessionRequestDao, listingDao, userDao, creditService, badgeService, draftService);
    }

    private User createTestUser(String prefix, int credits) {
        String email = prefix + "_" + System.nanoTime() + "@skillswap.local";
        Student s = new Student();
        s.setFullName("User " + prefix);
        s.setEmail(email);
        s.setPasswordHash(PasswordHasher.hash("Pass1234"));
        s.setRole(Role.STUDENT);
        s.setActive(true);
        s.setCredits(credits);
        s.setDepartment("Computer Science");
        s.setStudyYear(3);
        long id = userDao.insert(s);
        s.setId(id);
        return s;
    }

    private OfferedSkill createListing(long providerId, int skillId, int creditsPerHour) {
        OfferedSkill off = new OfferedSkill();
        off.setUserId(providerId);
        off.setSkillId(skillId);
        off.setLevel(SkillLevel.INTERMEDIATE);
        off.setCreditsPerHour(creditsPerHour);
        off.setDescription("Teaching session test");
        off.setActive(true);
        long listingId = listingDao.insert(off);
        off.setId(listingId);
        return off;
    }

    @Test
    public void testHappyPathSessionLifeCycleAndEscrow() throws Exception {
        User learner = createTestUser("learnerA", 10);
        User provider = createTestUser("providerA", 5);
        OfferedSkill listing = createListing(provider.getId(), 1, 2); // 2 credits/hr

        // 1. Learner requests 60 mins -> cost 2 credits
        LocalDateTime start = LocalDateTime.now().plusHours(5);
        SessionRequest req = sessionService.create(learner.getId(), listing.getId(), start, 60,
                SessionMode.ONLINE, "https://meet.google.com/abc", "Let's learn Java");

        assertNotNull(req);
        assertEquals(SessionStatus.PENDING, req.getStatus());
        assertEquals(2, req.getCostCredits());

        // Balance check: learner had 10, now 8; provider has 5
        assertEquals(8, creditService.getBalance(learner.getId()));
        assertEquals(5, creditService.getBalance(provider.getId()));
        assertEquals(2, creditService.getHeldCredits(learner.getId()));

        // 2. Provider accepts
        SessionRequest accepted = sessionService.accept(provider.getId(), req.getId());
        assertEquals(SessionStatus.ACCEPTED, accepted.getStatus());

        // 3. Confirm completion (simulate after start time)
        // Manually update proposedStart to 10 mins ago to simulate start time passed
        Database.getInstance().getConnection().createStatement().executeUpdate(
                "UPDATE session_requests SET proposed_start = DATE_SUB(NOW(), INTERVAL 10 MINUTE) WHERE id = " + req.getId()
        );

        SessionRequest conf1 = sessionService.confirm(learner.getId(), req.getId());
        assertEquals(SessionStatus.ACCEPTED, conf1.getStatus()); // only one confirmed

        SessionRequest conf2 = sessionService.confirm(provider.getId(), req.getId());
        assertEquals(SessionStatus.COMPLETED, conf2.getStatus()); // both confirmed!

        // Provider is now paid 2 credits
        assertEquals(8, creditService.getBalance(learner.getId()));
        assertEquals(7, creditService.getBalance(provider.getId())); // 5 + 2 = 7
        assertEquals(0, creditService.getHeldCredits(learner.getId()));

        // Check stats incremented
        User updatedLearner = userDao.findById(learner.getId()).orElseThrow();
        User updatedProvider = userDao.findById(provider.getId()).orElseThrow();
        assertEquals(1, updatedLearner.getSessionsLearned());
        assertEquals(1, updatedProvider.getSessionsTaught());
    }

    @Test
    public void testProviderRejectRefundsEscrow() throws Exception {
        User learner = createTestUser("learnerB", 10);
        User provider = createTestUser("providerB", 0);
        OfferedSkill listing = createListing(provider.getId(), 2, 3); // 3 credits/hr

        SessionRequest req = sessionService.create(learner.getId(), listing.getId(),
                LocalDateTime.now().plusHours(4), 60, SessionMode.ONLINE, "link", "help");
        assertEquals(7, creditService.getBalance(learner.getId())); // 10 - 3 = 7

        SessionRequest rejected = sessionService.reject(provider.getId(), req.getId());
        assertEquals(SessionStatus.REJECTED, rejected.getStatus());

        // Learner refunded in full
        assertEquals(10, creditService.getBalance(learner.getId()));
        assertEquals(0, creditService.getBalance(provider.getId()));
    }

    @Test
    public void testLearnerCancelPendingRefundsEscrow() throws Exception {
        User learner = createTestUser("learnerC", 10);
        User provider = createTestUser("providerC", 0);
        OfferedSkill listing = createListing(provider.getId(), 3, 2);

        SessionRequest req = sessionService.create(learner.getId(), listing.getId(),
                LocalDateTime.now().plusHours(6), 60, SessionMode.ONLINE, "link", "help");
        assertEquals(8, creditService.getBalance(learner.getId()));

        SessionRequest cancelled = sessionService.cancel(learner.getId(), req.getId());
        assertEquals(SessionStatus.CANCELLED, cancelled.getStatus());

        // Full refund
        assertEquals(10, creditService.getBalance(learner.getId()));
    }

    @Test
    public void testLearnerCancelAcceptedWithin2HoursSplitsCredits() throws Exception {
        User learner = createTestUser("learnerD", 10);
        User provider = createTestUser("providerD", 0);
        OfferedSkill listing = createListing(provider.getId(), 4, 4); // 4 credits

        SessionRequest req = sessionService.create(learner.getId(), listing.getId(),
                LocalDateTime.now().plusHours(5), 60, SessionMode.ONLINE, "link", "help");
        sessionService.accept(provider.getId(), req.getId());

        // Learner had 10, held 4 -> balance 6
        assertEquals(6, creditService.getBalance(learner.getId()));

        // Update proposed start to only 30 minutes in future (< 2 hours)
        Database.getInstance().getConnection().createStatement().executeUpdate(
                "UPDATE session_requests SET proposed_start = DATE_ADD(NOW(), INTERVAL 30 MINUTE) WHERE id = " + req.getId()
        );

        SessionRequest cancelled = sessionService.cancel(learner.getId(), req.getId());
        assertEquals(SessionStatus.CANCELLED, cancelled.getStatus());

        // Late cancellation: refund = cost / 2 = 2. Learner gets 2 back (6 + 2 = 8). Provider paid 2 (0 + 2 = 2).
        assertEquals(8, creditService.getBalance(learner.getId()));
        assertEquals(2, creditService.getBalance(provider.getId()));
    }

    @Test
    public void testLearnerCancelAcceptedMoreThan2HoursFullRefund() throws Exception {
        User learner = createTestUser("learnerE", 10);
        User provider = createTestUser("providerE", 0);
        OfferedSkill listing = createListing(provider.getId(), 5, 4);

        SessionRequest req = sessionService.create(learner.getId(), listing.getId(),
                LocalDateTime.now().plusHours(10), 60, SessionMode.ONLINE, "link", "help");
        sessionService.accept(provider.getId(), req.getId());

        assertEquals(6, creditService.getBalance(learner.getId()));

        // Cancelled with > 2h remaining -> full refund to learner, provider gets 0
        SessionRequest cancelled = sessionService.cancel(learner.getId(), req.getId());
        assertEquals(SessionStatus.CANCELLED, cancelled.getStatus());

        assertEquals(10, creditService.getBalance(learner.getId()));
        assertEquals(0, creditService.getBalance(provider.getId()));
    }

    @Test
    public void testProviderNoShowAwardsStrikeAndRefundsLearner() throws Exception {
        User learner = createTestUser("learnerF", 10);
        User provider = createTestUser("providerF", 0);
        OfferedSkill listing = createListing(provider.getId(), 6, 2);

        SessionRequest req = sessionService.create(learner.getId(), listing.getId(),
                LocalDateTime.now().plusHours(3), 60, SessionMode.ONLINE, "link", "help");
        sessionService.accept(provider.getId(), req.getId());

        // Simulate start time 20 mins ago (15+ min allowed for no-show)
        Database.getInstance().getConnection().createStatement().executeUpdate(
                "UPDATE session_requests SET proposed_start = DATE_SUB(NOW(), INTERVAL 20 MINUTE) WHERE id = " + req.getId()
        );

        SessionRequest noshow = sessionService.markNoShow(learner.getId(), req.getId());
        assertEquals(SessionStatus.NO_SHOW_PROVIDER, noshow.getStatus());

        // Learner gets full refund
        assertEquals(10, creditService.getBalance(learner.getId()));
        assertEquals(0, creditService.getBalance(provider.getId()));

        // Provider receives reliability strike
        User updatedProvider = userDao.findById(provider.getId()).orElseThrow();
        assertEquals(1, updatedProvider.getReliabilityStrikes());
    }

    @Test
    public void testLearnerNoShowCompensatesProvider() throws Exception {
        User learner = createTestUser("learnerG", 10);
        User provider = createTestUser("providerG", 0);
        OfferedSkill listing = createListing(provider.getId(), 7, 3);

        SessionRequest req = sessionService.create(learner.getId(), listing.getId(),
                LocalDateTime.now().plusHours(3), 60, SessionMode.ONLINE, "link", "help");
        sessionService.accept(provider.getId(), req.getId());

        // Simulate start time 25 mins ago
        Database.getInstance().getConnection().createStatement().executeUpdate(
                "UPDATE session_requests SET proposed_start = DATE_SUB(NOW(), INTERVAL 25 MINUTE) WHERE id = " + req.getId()
        );

        SessionRequest noshow = sessionService.markNoShow(provider.getId(), req.getId());
        assertEquals(SessionStatus.NO_SHOW_LEARNER, noshow.getStatus());

        // Learner does NOT get refunded, Provider gets paid
        assertEquals(7, creditService.getBalance(learner.getId()));
        assertEquals(3, creditService.getBalance(provider.getId()));
    }

    @Test
    public void testDraftRoundTrip() {
        long userId = 99999;
        long listingId = 88888;

        RequestDraft draft = new RequestDraft();
        draft.setProviderListingId(listingId);
        draft.setProposedStart("2026-10-25T14:00");
        draft.setDurationMinutes(90);
        draft.setMode(SessionMode.IN_PERSON);
        draft.setMeetingDetails("Library Study Room 3B");
        draft.setMessage("Let's review data structures");

        draftService.save(userId, listingId, draft);

        Optional<RequestDraft> loaded = draftService.load(userId, listingId);
        assertTrue(loaded.isPresent());
        assertEquals("2026-10-25T14:00", loaded.get().getProposedStart());
        assertEquals(90, loaded.get().getDurationMinutes());
        assertEquals(SessionMode.IN_PERSON, loaded.get().getMode());
        assertEquals("Library Study Room 3B", loaded.get().getMeetingDetails());
        assertEquals("Let's review data structures", loaded.get().getMessage());

        draftService.delete(userId, listingId);
        Optional<RequestDraft> deleted = draftService.load(userId, listingId);
        assertFalse(deleted.isPresent());
    }

    @Test
    public void testConcurrentAcceptExactlyOneWinner() throws Exception {
        User learner = createTestUser("learnerH", 10);
        User provider = createTestUser("providerH", 0);
        OfferedSkill listing = createListing(provider.getId(), 8, 2);

        SessionRequest req = sessionService.create(learner.getId(), listing.getId(),
                LocalDateTime.now().plusHours(4), 60, SessionMode.ONLINE, "link", "msg");

        int numThreads = 2;
        ExecutorService exec = Executors.newFixedThreadPool(numThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(numThreads);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        for (int i = 0; i < numThreads; i++) {
            exec.submit(() -> {
                try {
                    startLatch.await();
                    sessionService.accept(provider.getId(), req.getId());
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        doneLatch.await();
        exec.shutdown();

        assertEquals(1, successCount.get(), "Exactly one accept must succeed");
        assertEquals(1, failureCount.get(), "The second accept must fail due to guarded transition");

        SessionRequest finalReq = sessionService.getById(req.getId()).orElseThrow();
        assertEquals(SessionStatus.ACCEPTED, finalReq.getStatus());
    }

    @Test
    public void testConcurrentRequestsCannotExceedBalance() throws Exception {
        // Learner has 5 credits. Two threads try to create requests costing 4 credits each (4 + 4 = 8 > 5)
        User learner = createTestUser("learnerRace", 5);
        User provider1 = createTestUser("providerRace1", 0);
        User provider2 = createTestUser("providerRace2", 0);

        OfferedSkill listing1 = createListing(provider1.getId(), 1, 4); // 4 credits
        OfferedSkill listing2 = createListing(provider2.getId(), 2, 4); // 4 credits

        int numThreads = 2;
        ExecutorService exec = Executors.newFixedThreadPool(numThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(numThreads);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        exec.submit(() -> {
            try {
                startLatch.await();
                sessionService.create(learner.getId(), listing1.getId(),
                        LocalDateTime.now().plusHours(4), 60, SessionMode.ONLINE, "link1", "test1");
                successCount.incrementAndGet();
            } catch (Exception e) {
                failureCount.incrementAndGet();
            } finally {
                doneLatch.countDown();
            }
        });

        exec.submit(() -> {
            try {
                startLatch.await();
                sessionService.create(learner.getId(), listing2.getId(),
                        LocalDateTime.now().plusHours(4), 60, SessionMode.ONLINE, "link2", "test2");
                successCount.incrementAndGet();
            } catch (Exception e) {
                failureCount.incrementAndGet();
            } finally {
                doneLatch.countDown();
            }
        });

        startLatch.countDown();
        doneLatch.await();
        exec.shutdown();

        assertEquals(1, successCount.get(), "Only one request should succeed within credit balance");
        assertEquals(1, failureCount.get(), "The second request must fail with InsufficientCreditsException");

        int finalBalance = creditService.getBalance(learner.getId());
        assertEquals(1, finalBalance, "Balance should be 5 - 4 = 1");
        assertTrue(finalBalance >= 0, "Balance must never drop below 0");
    }
}
