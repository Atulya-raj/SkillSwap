package com.skillswap.service;

import com.skillswap.db.Database;
import com.skillswap.exception.ValidationException;
import com.skillswap.io.FileStorage;
import com.skillswap.model.Student;
import com.skillswap.model.User;
import com.skillswap.model.enums.Role;
import com.skillswap.model.enums.SessionMode;
import com.skillswap.model.enums.SkillLevel;
import com.skillswap.util.Config;
import com.skillswap.util.PasswordHasher;
import com.skillswap.util.Validator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration and unit tests for Profile, Skill Listings, Availability slots, and Avatar uploads.
 */
public class ProfileListingAvailabilityTest {

    private static ProfileService profileService;
    private static ListingService listingService;
    private static AvailabilityService availabilityService;
    private static CatalogService catalogService;
    private static long testUserId;

    @BeforeAll
    public static void setUp() {
        System.setProperty("config.file", "config-test.properties");
        Config.loadDefault();
        Database.init();

        profileService = ProfileService.getInstance();
        listingService = ListingService.getInstance();
        availabilityService = AvailabilityService.getInstance();
        catalogService = CatalogService.getInstance();
        catalogService.refreshCache();

        // Create a test student user
        String email = "phase5_test_" + System.currentTimeMillis() + "@skillswap.local";
        Student student = new Student();
        student.setFullName("Phase5 Tester");
        student.setEmail(email);
        student.setPasswordHash(PasswordHasher.hash("TestPass123"));
        student.setRole(Role.STUDENT);
        student.setActive(true);
        student.setCredits(10);

        com.skillswap.dao.UserDao userDao = new com.skillswap.dao.jdbc.JdbcUserDao();
        testUserId = userDao.insert(student);
    }

    @AfterAll
    public static void tearDown() {
        Database.getInstance().shutdown();
    }

    @Test
    public void testProfileUpdateValidAndInvalid() throws Exception {
        // Valid update
        User updated = profileService.update(testUserId, "Updated Name", "Computer Science", 3, "Love algorithms");
        assertNotNull(updated);
        assertTrue(updated.getFullName().contains("Updated Name"));

        // Invalid update: name too short
        assertThrows(ValidationException.class, () ->
                profileService.update(testUserId, "A", "CS", 2, "bio"));

        // Invalid update: year out of range
        assertThrows(ValidationException.class, () ->
                profileService.update(testUserId, "Valid Name", "CS", 6, "bio"));
    }

    @Test
    public void testListingHourlyRateBounds() {
        // Hourly rate < 1 should fail
        assertThrows(ValidationException.class, () ->
                listingService.offer(testUserId, 1, SkillLevel.BEGINNER, 0, "Too cheap"));

        // Hourly rate > 5 should fail
        assertThrows(ValidationException.class, () ->
                listingService.offer(testUserId, 1, SkillLevel.BEGINNER, 6, "Too expensive"));

        // Hourly rate 1..5 should succeed
        assertDoesNotThrow(() ->
                listingService.offer(testUserId, 1, SkillLevel.INTERMEDIATE, 3, "Fair price"));
    }

    @Test
    public void testDuplicateListingRejection() throws Exception {
        // We already offered skill 1 in the test above (or offer skill 2 here)
        listingService.offer(testUserId, 2, SkillLevel.ADVANCED, 4, "Advanced instruction");

        // Attempting to offer skill 2 again must be rejected
        ValidationException ex = assertThrows(ValidationException.class, () ->
                listingService.offer(testUserId, 2, SkillLevel.BEGINNER, 2, "Duplicate offer"));
        assertTrue(ex.getFieldErrors().containsKey("skillId"));

        // Test wanted duplicate rejection
        listingService.want(testUserId, 3, SkillLevel.ADVANCED, "Need help with Python");

        ValidationException ex2 = assertThrows(ValidationException.class, () ->
                listingService.want(testUserId, 3, SkillLevel.BEGINNER, "Duplicate want"));
        assertTrue(ex2.getFieldErrors().containsKey("skillId"));
    }

    @Test
    public void testAvailabilityValidAndOverlapRejection() throws Exception {
        // Add slot: Monday 09:00 - 11:00
        long slot1 = availabilityService.add(testUserId, DayOfWeek.MONDAY,
                LocalTime.of(9, 0), LocalTime.of(11, 0), SessionMode.ONLINE);
        assertTrue(slot1 > 0);

        // Attempting to add an overlapping slot on Monday (e.g. 10:00 - 12:00) must fail
        ValidationException overlapEx1 = assertThrows(ValidationException.class, () ->
                availabilityService.add(testUserId, DayOfWeek.MONDAY,
                        LocalTime.of(10, 0), LocalTime.of(12, 0), SessionMode.IN_PERSON));
        assertTrue(overlapEx1.getFieldErrors().containsKey("time"));

        // Attempting to add slot where end <= start must fail
        ValidationException invalidTimeEx = assertThrows(ValidationException.class, () ->
                availabilityService.add(testUserId, DayOfWeek.TUESDAY,
                        LocalTime.of(14, 0), LocalTime.of(13, 0), SessionMode.ONLINE));
        assertTrue(invalidTimeEx.getFieldErrors().containsKey("endTime"));

        // Non-overlapping slot on Monday (e.g. 14:00 - 16:00) should succeed
        assertDoesNotThrow(() ->
                availabilityService.add(testUserId, DayOfWeek.MONDAY,
                        LocalTime.of(14, 0), LocalTime.of(16, 0), SessionMode.ONLINE));
    }

    @Test
    public void testAvatarUploadRejectsNonImageAndRenamedFiles() {
        FileStorage storage = FileStorage.getInstance();

        // 1. A text file named avatar.txt
        byte[] textBytes = "This is a plain text file, not an image".getBytes(StandardCharsets.UTF_8);
        InputStream textIn = new ByteArrayInputStream(textBytes);
        assertThrows(ValidationException.class, () ->
                storage.saveAvatar("avatar.txt", textIn, textBytes.length, null));

        // 2. A renamed text file named avatar.jpg with text contents (fails magic bytes)
        InputStream fakeJpgIn = new ByteArrayInputStream(textBytes);
        ValidationException magicEx = assertThrows(ValidationException.class, () ->
                storage.saveAvatar("avatar.jpg", fakeJpgIn, textBytes.length, null));
        assertTrue(magicEx.getFieldErrors().containsKey("avatar"));

        // 3. A valid JPEG header (FF D8 FF E0 ...)
        byte[] validJpegBytes = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0x00, 0x10, 0x4A, 0x46, 0x49, 0x46};
        InputStream validJpegIn = new ByteArrayInputStream(validJpegBytes);
        assertDoesNotThrow(() -> {
            String saved = storage.saveAvatar("photo.jpg", validJpegIn, validJpegBytes.length, null);
            assertNotNull(saved);
            assertTrue(saved.endsWith(".jpg"));
            // Clean up
            storage.deleteAvatar(saved);
        });

        // 4. A valid PNG header (89 50 4E 47 0D 0A 1A 0A)
        byte[] validPngBytes = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x00};
        InputStream validPngIn = new ByteArrayInputStream(validPngBytes);
        assertDoesNotThrow(() -> {
            String saved = storage.saveAvatar("photo.png", validPngIn, validPngBytes.length, null);
            assertNotNull(saved);
            assertTrue(saved.endsWith(".png"));
            // Clean up
            storage.deleteAvatar(saved);
        });
    }
}
