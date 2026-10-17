package com.skillswap.test;

import com.skillswap.dao.jdbc.JdbcListingDao;
import com.skillswap.dao.jdbc.JdbcSessionRequestDao;
import com.skillswap.dao.jdbc.JdbcUserDao;
import com.skillswap.db.Database;
import com.skillswap.db.DatabaseInitializer;
import com.skillswap.model.OfferedSkill;
import com.skillswap.model.SessionRequest;
import com.skillswap.model.Student;
import com.skillswap.model.WantedSkill;
import com.skillswap.model.enums.Role;
import com.skillswap.model.enums.SessionMode;
import com.skillswap.model.enums.SessionStatus;
import com.skillswap.model.enums.SkillLevel;
import com.skillswap.util.PasswordHasher;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;

/**
 * Test helper for database integration tests providing fixtures and clean-up.
 */
public class TestData {

    private static boolean initialized = false;

    /**
     * Initializes test database schema and seeds once.
     */
    public static synchronized void initTestDatabase() {
        if (!initialized) {
            new DatabaseInitializer().initialize();
            initialized = true;
        }
    }

    /**
     * Cleans dynamic data from all application tables in foreign key order.
     */
    public static void cleanData() {
        initTestDatabase();
        try (Connection con = Database.getInstance().getConnection();
             Statement stmt = con.createStatement()) {
            stmt.executeUpdate("DELETE FROM reports");
            stmt.executeUpdate("DELETE FROM user_badges");
            stmt.executeUpdate("DELETE FROM messages");
            stmt.executeUpdate("DELETE FROM notifications");
            stmt.executeUpdate("DELETE FROM reviews");
            stmt.executeUpdate("DELETE FROM credits_ledger");
            stmt.executeUpdate("DELETE FROM session_requests");
            stmt.executeUpdate("DELETE FROM availability_slots");
            stmt.executeUpdate("DELETE FROM user_skills");
            stmt.executeUpdate("DELETE FROM users WHERE role = 'STUDENT'");
        } catch (SQLException e) {
            throw new RuntimeException("Failed to clean test data", e);
        }
    }

    /**
     * Creates and persists a test student.
     */
    public static Student insertStudent(String name, String email, String password, int credits) {
        JdbcUserDao userDao = new JdbcUserDao();
        Student s = new Student();
        s.setName(name);
        s.setEmail(email);
        s.setPasswordHash(PasswordHasher.hash(password));
        s.setCredits(credits);
        s.setRole(Role.STUDENT);
        s.setActive(true);
        s.setDepartment("Computer Science");
        s.setBio("Test bio for " + name);
        long id = userDao.insert(s);
        s.setId(id);
        return s;
    }

    /**
     * Creates and persists an offered skill.
     */
    public static OfferedSkill insertOffered(long userId, int skillId, int hourlyRate, SkillLevel level) {
        JdbcListingDao listingDao = new JdbcListingDao();
        OfferedSkill skill = new OfferedSkill();
        skill.setUserId(userId);
        skill.setSkillId(skillId);
        skill.setHourlyRate(hourlyRate);
        skill.setLevel(level);
        skill.setDescription("Offering test skill " + skillId);
        long id = listingDao.insert(skill);
        skill.setId(id);
        return skill;
    }

    /**
     * Creates and persists a wanted skill.
     */
    public static WantedSkill insertWanted(long userId, int skillId, SkillLevel level) {
        JdbcListingDao listingDao = new JdbcListingDao();
        WantedSkill skill = new WantedSkill();
        skill.setUserId(userId);
        skill.setSkillId(skillId);
        skill.setLevel(level);
        skill.setDescription("Wanting test skill " + skillId);
        long id = listingDao.insert(skill);
        skill.setId(id);
        return skill;
    }

    /**
     * Creates and persists a session request.
     */
    public static SessionRequest insertSession(long learnerId, long providerId, int skillId, long listingId,
                                                LocalDateTime start, int durationMinutes, SessionStatus status) {
        JdbcSessionRequestDao sessionDao = new JdbcSessionRequestDao();
        SessionRequest sr = new SessionRequest();
        sr.setLearnerId(learnerId);
        sr.setProviderId(providerId);
        sr.setSkillId(skillId);
        sr.setListingId(listingId);
        sr.setProposedStart(start);
        sr.setDurationMinutes(durationMinutes);
        sr.setMode(SessionMode.ONLINE);
        sr.setMeetingDetails("https://meet.jit.si/test-room");
        sr.setMessage("Test session message");
        sr.setCostCredits(10);
        sr.setStatus(status);
        long id = sessionDao.insert(sr);
        sr.setId(id);
        return sr;
    }
}
