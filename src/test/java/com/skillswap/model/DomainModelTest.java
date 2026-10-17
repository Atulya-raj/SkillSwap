package com.skillswap.model;

import com.skillswap.exception.ValidationException;
import com.skillswap.model.enums.ListingType;
import com.skillswap.model.enums.SessionStatus;
import com.skillswap.model.enums.SkillLevel;
import com.skillswap.util.PasswordHasher;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests verifying domain models, lifecycle state transitions, validation, and security utilities.
 */
public class DomainModelTest {

    @Test
    public void testPasswordHasher() {
        String password = "StrongPassword123";
        String hash1 = PasswordHasher.hash(password);
        String hash2 = PasswordHasher.hash(password);

        assertNotNull(hash1);
        assertNotNull(hash2);
        assertNotEquals(hash1, hash2, "Different salts must produce different hash strings");

        assertTrue(PasswordHasher.verify(password, hash1), "Correct password must verify against hash1");
        assertTrue(PasswordHasher.verify(password, hash2), "Correct password must verify against hash2");
        assertFalse(PasswordHasher.verify("WrongPassword123", hash1), "Incorrect password must fail verification");
        assertFalse(PasswordHasher.verify("", hash1), "Empty password must fail verification");
        assertFalse(PasswordHasher.verify(null, hash1), "Null password must return false");
        assertFalse(PasswordHasher.verify(password, null), "Null hash must return false");
    }

    @Test
    public void testSessionStatusStateTransitions() {
        // Valid transitions from PENDING
        assertTrue(SessionStatus.PENDING.canMoveTo(SessionStatus.ACCEPTED));
        assertTrue(SessionStatus.PENDING.canMoveTo(SessionStatus.REJECTED));
        assertTrue(SessionStatus.PENDING.canMoveTo(SessionStatus.CANCELLED));
        assertTrue(SessionStatus.PENDING.canMoveTo(SessionStatus.EXPIRED));

        // Invalid transitions from PENDING
        assertFalse(SessionStatus.PENDING.canMoveTo(SessionStatus.COMPLETED));
        assertFalse(SessionStatus.PENDING.canMoveTo(SessionStatus.NO_SHOW_LEARNER));
        assertFalse(SessionStatus.PENDING.canMoveTo(SessionStatus.NO_SHOW_PROVIDER));
        assertFalse(SessionStatus.PENDING.canMoveTo(SessionStatus.PENDING));

        // Valid transitions from ACCEPTED
        assertTrue(SessionStatus.ACCEPTED.canMoveTo(SessionStatus.COMPLETED));
        assertTrue(SessionStatus.ACCEPTED.canMoveTo(SessionStatus.CANCELLED));
        assertTrue(SessionStatus.ACCEPTED.canMoveTo(SessionStatus.NO_SHOW_LEARNER));
        assertTrue(SessionStatus.ACCEPTED.canMoveTo(SessionStatus.NO_SHOW_PROVIDER));

        // Invalid transitions from ACCEPTED
        assertFalse(SessionStatus.ACCEPTED.canMoveTo(SessionStatus.PENDING));
        assertFalse(SessionStatus.ACCEPTED.canMoveTo(SessionStatus.REJECTED));
        assertFalse(SessionStatus.ACCEPTED.canMoveTo(SessionStatus.EXPIRED));
        assertFalse(SessionStatus.ACCEPTED.canMoveTo(SessionStatus.ACCEPTED));

        // Terminal states cannot transition to ANY state
        EnumSet<SessionStatus> terminalStates = EnumSet.of(
                SessionStatus.REJECTED, SessionStatus.CANCELLED, SessionStatus.EXPIRED,
                SessionStatus.COMPLETED, SessionStatus.NO_SHOW_LEARNER, SessionStatus.NO_SHOW_PROVIDER
        );

        for (SessionStatus terminal : terminalStates) {
            assertTrue(terminal.isTerminal(), terminal + " must be terminal");
            for (SessionStatus target : SessionStatus.values()) {
                assertFalse(terminal.canMoveTo(target),
                        "Terminal state " + terminal + " must not transition to " + target);
            }
        }

        assertFalse(SessionStatus.PENDING.isTerminal());
        assertFalse(SessionStatus.ACCEPTED.isTerminal());
    }

    @Test
    public void testOfferedSkillValidationBounds() {
        OfferedSkill skill = new OfferedSkill();
        skill.setSkillId(1);
        skill.setLevel(SkillLevel.INTERMEDIATE);
        skill.setCreditsPerHour(3);
        skill.setSkillName("Java");

        assertDoesNotThrow(skill::validate);
        assertEquals(ListingType.OFFERED, skill.type());
        assertTrue(skill.describe().contains("Teaches Java (INTERMEDIATE) at 3 credits/hour"));

        // Upper and lower bounds for creditsPerHour (1..5)
        skill.setCreditsPerHour(1);
        assertDoesNotThrow(skill::validate);

        skill.setCreditsPerHour(5);
        assertDoesNotThrow(skill::validate);

        skill.setCreditsPerHour(0);
        assertThrows(ValidationException.class, skill::validate);

        skill.setCreditsPerHour(6);
        assertThrows(ValidationException.class, skill::validate);

        // Missing level
        skill.setCreditsPerHour(3);
        skill.setLevel(null);
        assertThrows(ValidationException.class, skill::validate);

        // Invalid skill ID
        skill.setLevel(SkillLevel.BEGINNER);
        skill.setSkillId(0);
        assertThrows(ValidationException.class, skill::validate);
    }

    @Test
    public void testAvailabilitySlotOverlapMinutes() {
        AvailabilitySlot slotA = new AvailabilitySlot(1L, DayOfWeek.MONDAY, LocalTime.of(10, 0), LocalTime.of(12, 0));
        AvailabilitySlot slotB = new AvailabilitySlot(2L, DayOfWeek.MONDAY, LocalTime.of(11, 0), LocalTime.of(13, 0));

        // 11:00 to 12:00 = 60 minutes overlap
        assertEquals(60, slotA.overlapMinutes(slotB));
        assertEquals(60, slotB.overlapMinutes(slotA));

        // Disjoint on same day
        AvailabilitySlot slotC = new AvailabilitySlot(3L, DayOfWeek.MONDAY, LocalTime.of(14, 0), LocalTime.of(16, 0));
        assertEquals(0, slotA.overlapMinutes(slotC));

        // Contiguous (touching boundaries) -> 0 minutes
        AvailabilitySlot slotTouch = new AvailabilitySlot(4L, DayOfWeek.MONDAY, LocalTime.of(12, 0), LocalTime.of(14, 0));
        assertEquals(0, slotA.overlapMinutes(slotTouch));

        // Same times but different day -> 0 minutes
        AvailabilitySlot slotTuesday = new AvailabilitySlot(5L, DayOfWeek.TUESDAY, LocalTime.of(10, 0), LocalTime.of(12, 0));
        assertEquals(0, slotA.overlapMinutes(slotTuesday));

        // Full containment
        AvailabilitySlot slotInner = new AvailabilitySlot(6L, DayOfWeek.MONDAY, LocalTime.of(10, 30), LocalTime.of(11, 30));
        assertEquals(60, slotA.overlapMinutes(slotInner));
    }
}
