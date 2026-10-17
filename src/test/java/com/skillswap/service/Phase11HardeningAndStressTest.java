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
import com.skillswap.db.DemoDataLoader;
import com.skillswap.exception.InsufficientCreditsException;
import com.skillswap.exception.SkillSwapException;
import com.skillswap.exception.ValidationException;
import com.skillswap.model.OfferedSkill;
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

import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Phase 11 hardening, negative validation paths, auto-expiry/auto-complete, and multi-threaded stress tests.
 */
public class Phase11HardeningAndStressTest {

    private static SessionService sessionService;
    private static CreditService creditService;
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
        DraftService draftService = new DraftService();
        BadgeService badgeService = new BadgeService();
        sessionService = new SessionService(sessionRequestDao, listingDao, userDao, creditService, badgeService, draftService);
    }

    private User createUser(String prefix, int credits) {
        String email = prefix + "_" + System.nanoTime() + "@skillswap.local";
        Student s = new Student();
        s.setFullName("Student " + prefix);
        s.setEmail(email);
        s.setPasswordHash(PasswordHasher.hash("Demo1234!"));
        s.setRole(Role.STUDENT);
        s.setActive(true);
        s.setCredits(credits);
        s.setDepartment("Computer Science");
        s.setStudyYear(2);
        long id = userDao.insert(s);
        s.setId(id);
        return s;
    }

    private OfferedSkill createListing(long userId, int skillId, int rate, boolean active) {
        OfferedSkill off = new OfferedSkill();
        off.setUserId(userId);
        off.setSkillId(skillId);
        off.setLevel(SkillLevel.INTERMEDIATE);
        off.setCreditsPerHour(rate);
        off.setDescription("Listing for test");
        off.setActive(active);
        long id = listingDao.insert(off);
        off.setId(id);
        return off;
    }

    @Test
    public void testCannotRequestOwnListing() {
        User user = createUser("selfReq", 20);
        OfferedSkill listing = createListing(user.getId(), 1, 2, true);

        assertThrows(ValidationException.class, () ->
                sessionService.create(user.getId(), listing.getId(), LocalDateTime.now().plusDays(1), 60,
                        SessionMode.ONLINE, "link", "Self test"));
    }

    @Test
    public void testCannotRequestInactiveListing() {
        User learner = createUser("learnerInactive", 20);
        User provider = createUser("providerInactive", 10);
        OfferedSkill listing = createListing(provider.getId(), 1, 2, false);

        assertThrows(ValidationException.class, () ->
                sessionService.create(learner.getId(), listing.getId(), LocalDateTime.now().plusDays(1), 60,
                        SessionMode.ONLINE, "link", "Inactive test"));
    }

    @Test
    public void testInsufficientCreditsThrowsException() {
        User learner = createUser("poorLearner", 1); // 1 credit
        User provider = createUser("richProvider", 10);
        OfferedSkill listing = createListing(provider.getId(), 1, 3, true); // 3 credits/hr

        assertThrows(InsufficientCreditsException.class, () ->
                sessionService.create(learner.getId(), listing.getId(), LocalDateTime.now().plusDays(1), 60,
                        SessionMode.ONLINE, "link", "Insufficient credits"));
    }

    @Test
    public void testOverdueExpiryTaskRefundsHeldCredits() throws Exception {
        User learner = createUser("expiryLearner", 15);
        User provider = createUser("expiryProvider", 5);
        OfferedSkill listing = createListing(provider.getId(), 1, 3, true); // 3 credits

        SessionRequest sr = sessionService.create(learner.getId(), listing.getId(),
                LocalDateTime.now().plusDays(3), 60, SessionMode.ONLINE, "link", "Expiry test");
        assertEquals(12, creditService.getBalance(learner.getId())); // 15 - 3 = 12

        // Simulate created_at was 50 hours ago (> 48h limit)
        Database.getInstance().getConnection().createStatement().executeUpdate(
                "UPDATE session_requests SET created_at = DATE_SUB(NOW(), INTERVAL 50 HOUR) WHERE id = " + sr.getId()
        );

        int expiredCount = sessionService.expireStale();
        assertTrue(expiredCount >= 1, "Should have expired at least 1 request");

        SessionRequest updated = sessionService.getById(sr.getId()).orElseThrow();
        assertEquals(SessionStatus.EXPIRED, updated.getStatus());

        // Learner should be refunded
        assertEquals(15, creditService.getBalance(learner.getId()));
    }

    @Test
    public void testAutoCompleteTaskPaysProvider() throws Exception {
        User learner = createUser("autoCompLearner", 20);
        User provider = createUser("autoCompProvider", 0);
        OfferedSkill listing = createListing(provider.getId(), 1, 4, true); // 4 credits

        SessionRequest sr = sessionService.create(learner.getId(), listing.getId(),
                LocalDateTime.now().plusDays(1), 60, SessionMode.ONLINE, "link", "AutoComplete test");
        sessionService.accept(provider.getId(), sr.getId());

        // Update proposed start to in the past and first_confirmed_at to 50 hours ago
        Database.getInstance().getConnection().createStatement().executeUpdate(
                "UPDATE session_requests SET proposed_start = DATE_SUB(NOW(), INTERVAL 52 HOUR), " +
                        "learner_confirmed = TRUE, first_confirmed_at = DATE_SUB(NOW(), INTERVAL 50 HOUR) WHERE id = " + sr.getId()
        );

        int completedCount = sessionService.autoCompleteHalfConfirmed();
        assertTrue(completedCount >= 1, "Should have auto-completed at least 1 session");

        SessionRequest updated = sessionService.getById(sr.getId()).orElseThrow();
        assertEquals(SessionStatus.COMPLETED, updated.getStatus());

        // Provider paid 4 credits
        assertEquals(4, creditService.getBalance(provider.getId()));
    }

    @Test
    public void testStressConcurrency20Runs() throws Exception {
        // Run 20 iterations of concurrent double-accept and race condition handling
        for (int run = 1; run <= 20; run++) {
            User learner = createUser("stressL" + run, 10);
            User provider = createUser("stressP" + run, 0);
            OfferedSkill listing = createListing(provider.getId(), 1, 2, true);

            SessionRequest req = sessionService.create(learner.getId(), listing.getId(),
                    LocalDateTime.now().plusHours(5 + run), 60, SessionMode.ONLINE, "link", "Stress test " + run);

            int numThreads = 2;
            ExecutorService exec = Executors.newFixedThreadPool(numThreads);
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch doneLatch = new CountDownLatch(numThreads);
            AtomicInteger success = new AtomicInteger(0);
            AtomicInteger failed = new AtomicInteger(0);

            for (int t = 0; t < numThreads; t++) {
                exec.submit(() -> {
                    try {
                        startLatch.await();
                        sessionService.accept(provider.getId(), req.getId());
                        success.incrementAndGet();
                    } catch (Exception e) {
                        failed.incrementAndGet();
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }

            startLatch.countDown();
            doneLatch.await();
            exec.shutdown();

            assertEquals(1, success.get(), "Run #" + run + ": exactly 1 accept must succeed");
            assertEquals(1, failed.get(), "Run #" + run + ": the 2nd accept must fail cleanly");

            SessionRequest finalState = sessionService.getById(req.getId()).orElseThrow();
            assertEquals(SessionStatus.ACCEPTED, finalState.getStatus());
        }
    }

    @Test
    public void testDemoDataLoaderIntegration() {
        DemoDataLoader loader = new DemoDataLoader();
        // Force test execution
        loader.load();

        int studentCount = userDao.count("");
        assertTrue(studentCount >= 12, "Demo loader should have inserted at least 12 students");

        // Verify Aarav exists
        User aarav = userDao.findByEmail("aarav.sharma@vitstudent.ac.in").orElse(null);
        assertNotNull(aarav, "Aarav Sharma should exist in database");
        assertEquals("Computer Science & Engineering", aarav.getDepartment());
        assertTrue(aarav.getCredits() > 0);

        // Verify listings and slots exist
        assertTrue(!listingDao.findByUser(aarav.getId()).isEmpty(), "Aarav should have listings");
    }
}
