package com.skillswap.dao;

import com.skillswap.dao.jdbc.JdbcAvailabilityDao;
import com.skillswap.dao.jdbc.JdbcListingDao;
import com.skillswap.dao.jdbc.JdbcSessionRequestDao;
import com.skillswap.dao.jdbc.JdbcUserDao;
import com.skillswap.model.AvailabilitySlot;
import com.skillswap.model.OfferedSkill;
import com.skillswap.model.SessionRequest;
import com.skillswap.model.SkillListing;
import com.skillswap.model.Student;
import com.skillswap.model.User;
import com.skillswap.model.WantedSkill;
import com.skillswap.model.enums.ListingType;
import com.skillswap.model.enums.SessionStatus;
import com.skillswap.model.enums.SkillLevel;
import com.skillswap.test.TestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests verifying DAO operations against the skillswap_test schema.
 */
public class DaoIntegrationTest {

    private final UserDao userDao = new JdbcUserDao();
    private final ListingDao listingDao = new JdbcListingDao();
    private final SessionRequestDao sessionDao = new JdbcSessionRequestDao();
    private final AvailabilityDao availabilityDao = new JdbcAvailabilityDao();

    @BeforeEach
    public void setUp() {
        TestData.cleanData();
    }

    @Test
    @DisplayName("Insert and find user by id and email")
    public void testInsertAndFindUser() {
        Student s = TestData.insertStudent("Alice Walker", "alice@example.com", "Secret123!", 10);
        assertTrue(s.getId() > 0);

        Optional<User> foundById = userDao.findById(s.getId());
        assertTrue(foundById.isPresent());
        assertEquals("Alice Walker", foundById.get().getName());
        assertEquals("alice@example.com", foundById.get().getEmail());
        assertEquals(10, foundById.get().getCredits());

        Optional<User> foundByEmail = userDao.findByEmail("alice@example.com");
        assertTrue(foundByEmail.isPresent());
        assertEquals(s.getId(), foundByEmail.get().getId());
    }

    @Test
    @DisplayName("Insert both OFFERED and WANTED listings and read them back as right subclass")
    public void testPolymorphicListings() {
        Student bob = TestData.insertStudent("Bob Smith", "bob@example.com", "Secret123!", 10);

        // 1 is Java, 2 is Python in seed.sql
        OfferedSkill offered = TestData.insertOffered(bob.getId(), 1, 3, SkillLevel.ADVANCED);
        WantedSkill wanted = TestData.insertWanted(bob.getId(), 2, SkillLevel.BEGINNER);

        Optional<SkillListing> foundOffered = listingDao.findById(offered.getId());
        assertTrue(foundOffered.isPresent());
        assertInstanceOf(OfferedSkill.class, foundOffered.get());
        OfferedSkill os = (OfferedSkill) foundOffered.get();
        assertEquals(ListingType.OFFERED, os.getType());
        assertEquals(3, os.getHourlyRate());
        assertEquals(SkillLevel.ADVANCED, os.getLevel());

        Optional<SkillListing> foundWanted = listingDao.findById(wanted.getId());
        assertTrue(foundWanted.isPresent());
        assertInstanceOf(WantedSkill.class, foundWanted.get());
        WantedSkill ws = (WantedSkill) foundWanted.get();
        assertEquals(ListingType.WANTED, ws.getType());
        assertEquals(SkillLevel.BEGINNER, ws.getLevel());

        List<SkillListing> allForUser = listingDao.findByUser(bob.getId());
        assertEquals(2, allForUser.size());
    }

    @Test
    @DisplayName("Guarded status update succeeds on matching status and fails on stale status")
    public void testGuardedStatusUpdate() {
        Student learner = TestData.insertStudent("Learner", "learner@example.com", "Secret123!", 20);
        Student provider = TestData.insertStudent("Provider", "provider@example.com", "Secret123!", 20);
        OfferedSkill listing = TestData.insertOffered(provider.getId(), 1, 2, SkillLevel.INTERMEDIATE);

        LocalDateTime start = LocalDateTime.now().plusDays(2).withHour(14).withMinute(0).withSecond(0).withNano(0);
        SessionRequest sr = TestData.insertSession(learner.getId(), provider.getId(), 1, listing.getId(),
                start, 60, SessionStatus.PENDING);

        // Transition PENDING -> ACCEPTED should succeed
        boolean updated = sessionDao.updateStatus(sr.getId(), SessionStatus.PENDING, SessionStatus.ACCEPTED);
        assertTrue(updated, "Status update from PENDING to ACCEPTED must succeed");

        // Stale transition PENDING -> REJECTED must fail because status is now ACCEPTED
        boolean staleUpdate = sessionDao.updateStatus(sr.getId(), SessionStatus.PENDING, SessionStatus.REJECTED);
        assertFalse(staleUpdate, "Stale update from PENDING when current is ACCEPTED must return false");

        // Verify current status is ACCEPTED
        Optional<SessionRequest> reloaded = sessionDao.findById(sr.getId());
        assertTrue(reloaded.isPresent());
        assertEquals(SessionStatus.ACCEPTED, reloaded.get().getStatus());
    }

    @Test
    @DisplayName("hasOverlappingAccepted detects conflicts and handles edge boundaries")
    public void testHasOverlappingAcceptedEdges() {
        Student learner = TestData.insertStudent("Learner2", "learner2@example.com", "Secret123!", 20);
        Student provider = TestData.insertStudent("Provider2", "provider2@example.com", "Secret123!", 20);
        OfferedSkill listing = TestData.insertOffered(provider.getId(), 1, 2, SkillLevel.INTERMEDIATE);

        // Accepted session: tomorrow 10:00 to 11:00 (60 mins)
        LocalDateTime sessionStart = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0).withSecond(0).withNano(0);
        TestData.insertSession(learner.getId(), provider.getId(), 1, listing.getId(),
                sessionStart, 60, SessionStatus.ACCEPTED);

        // Overlapping 10:30 to 11:30 -> conflict
        LocalDateTime overlapStart = sessionStart.plusMinutes(30);
        boolean overlap1 = sessionDao.hasOverlappingAccepted(provider.getId(), overlapStart, 60, null);
        assertTrue(overlap1, "10:30-11:30 overlaps with 10:00-11:00");

        // Overlapping completely within 10:15 to 10:45 -> conflict
        boolean overlap2 = sessionDao.hasOverlappingAccepted(provider.getId(), sessionStart.plusMinutes(15), 30, null);
        assertTrue(overlap2, "10:15-10:45 overlaps with 10:00-11:00");

        // Edge test: exactly starts when existing ends: 11:00 to 12:00 -> NO conflict
        boolean edgeAfter = sessionDao.hasOverlappingAccepted(provider.getId(), sessionStart.plusMinutes(60), 60, null);
        assertFalse(edgeAfter, "11:00-12:00 does not conflict with 10:00-11:00");

        // Edge test: exactly ends when existing starts: 09:00 to 10:00 -> NO conflict
        boolean edgeBefore = sessionDao.hasOverlappingAccepted(provider.getId(), sessionStart.minusMinutes(60), 60, null);
        assertFalse(edgeBefore, "09:00-10:00 does not conflict with 10:00-11:00");
    }

    @Test
    @DisplayName("Availability slot hasOverlap detects conflicts and excludes specified slot")
    public void testAvailabilityOverlap() {
        Student user = TestData.insertStudent("AvailUser", "avail@example.com", "Secret123!", 10);
        AvailabilitySlot slot = new AvailabilitySlot(user.getId(), DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(12, 0));
        long slotId = availabilityDao.insert(slot);

        // Overlapping: 10:00 to 11:00
        boolean hasOverlap = availabilityDao.hasOverlap(user.getId(), DayOfWeek.MONDAY, LocalTime.of(10, 0), LocalTime.of(11, 0), null);
        assertTrue(hasOverlap);

        // Exclude the slot itself: updating slot 9:00 to 12:00 shouldn't conflict with itself
        boolean excludeSelf = availabilityDao.hasOverlap(user.getId(), DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(12, 0), slotId);
        assertFalse(excludeSelf);

        // Non-overlapping: 13:00 to 15:00
        boolean nonOverlap = availabilityDao.hasOverlap(user.getId(), DayOfWeek.MONDAY, LocalTime.of(13, 0), LocalTime.of(15, 0), null);
        assertFalse(nonOverlap);
    }
}
