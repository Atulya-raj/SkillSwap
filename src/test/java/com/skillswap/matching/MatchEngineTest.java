package com.skillswap.matching;

import com.skillswap.dao.AvailabilityDao;
import com.skillswap.dao.ListingDao;
import com.skillswap.dao.SkillDao;
import com.skillswap.dao.UserDao;
import com.skillswap.exception.NoMatchFoundException;
import com.skillswap.model.AvailabilitySlot;
import com.skillswap.model.MatchResult;
import com.skillswap.model.OfferedSkill;
import com.skillswap.model.Skill;
import com.skillswap.model.Student;
import com.skillswap.model.User;
import com.skillswap.model.enums.Role;
import com.skillswap.model.enums.SkillLevel;
import com.skillswap.util.Config;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit and scoring tests for WeightedMatchEngine, RatingFirstMatchEngine, and MatchEngineFactory.
 */
public class MatchEngineTest {

    private MockListingDao listingDao;
    private MockUserDao userDao;
    private MockSkillDao skillDao;
    private MockAvailabilityDao availabilityDao;
    private WeightedMatchEngine weightedEngine;

    @BeforeEach
    public void setUp() {
        Config.set("match.engine", "weighted");
        listingDao = new MockListingDao();
        userDao = new MockUserDao();
        skillDao = new MockSkillDao();
        availabilityDao = new MockAvailabilityDao();
        weightedEngine = new WeightedMatchEngine(listingDao, userDao, skillDao, availabilityDao);

        // Setup base catalog: Category 1 ("Programming") with Skills 10 ("Java") and 11 ("Python")
        Skill javaSkill = new Skill(10, 1, "Java");
        Skill pythonSkill = new Skill(11, 1, "Python");
        skillDao.skills.put(10, javaSkill);
        skillDao.skills.put(11, pythonSkill);
    }

    @Test
    public void testExactBeatsSameCategory() throws Exception {
        long learnerId = 100L;

        // Provider A offers exact skill (Java - 10)
        User userA = createUser(1L, "Provider Exact", new BigDecimal("4.00"), 5, true);
        OfferedSkill offerA = createOffer(101L, 1L, 10, 1, 3, true);

        // Provider B offers same-category skill (Python - 11 in Category 1)
        User userB = createUser(2L, "Provider SameCategory", new BigDecimal("4.00"), 5, true);
        OfferedSkill offerB = createOffer(102L, 2L, 11, 1, 3, true);

        userDao.users.put(1L, userA);
        userDao.users.put(2L, userB);
        listingDao.offered.add(offerA);
        listingDao.offered.add(offerB);

        List<MatchResult> matches = weightedEngine.findProviders(learnerId, 10, 10);
        assertEquals(2, matches.size());
        // Exact skill match must rank first
        assertEquals(1L, matches.get(0).getProvider().getId());
        assertEquals(1.0, matches.get(0).getSkillRelevance(), 0.001);

        assertEquals(2L, matches.get(1).getProvider().getId());
        assertEquals(0.4, matches.get(1).getSkillRelevance(), 0.001);

        assertTrue(matches.get(0).getScore() > matches.get(1).getScore());
    }

    @Test
    public void testHigherRatingBeatsLowerAtEqualRelevance() throws Exception {
        long learnerId = 100L;

        // Both offer exact skill 10
        User highRating = createUser(1L, "High Rating", new BigDecimal("5.00"), 10, true);
        OfferedSkill offer1 = createOffer(101L, 1L, 10, 1, 3, true);

        User lowerRating = createUser(2L, "Lower Rating", new BigDecimal("3.00"), 10, true);
        OfferedSkill offer2 = createOffer(102L, 2L, 10, 1, 3, true);

        userDao.users.put(1L, highRating);
        userDao.users.put(2L, lowerRating);
        listingDao.offered.add(offer1);
        listingDao.offered.add(offer2);

        List<MatchResult> matches = weightedEngine.findProviders(learnerId, 10, 10);
        assertEquals(2, matches.size());
        assertEquals(1L, matches.get(0).getProvider().getId());
        assertEquals(2L, matches.get(1).getProvider().getId());
        assertTrue(matches.get(0).getScore() > matches.get(1).getScore());
    }

    @Test
    public void testColdStartPriorAppliedUnderThreeRatings() throws Exception {
        long learnerId = 100L;

        // User with fewer than 3 ratings (cold start) -> ratingScore must be 0.6
        User coldStartUser = createUser(1L, "Cold Start", new BigDecimal("5.00"), 1, true);
        OfferedSkill offer = createOffer(101L, 1L, 10, 1, 3, true);

        userDao.users.put(1L, coldStartUser);
        listingDao.offered.add(offer);

        List<MatchResult> matches = weightedEngine.findProviders(learnerId, 10, 10);
        assertEquals(1, matches.size());
        assertEquals(0.6, matches.get(0).getRatingScore(), 0.001);
    }

    @Test
    public void testLearnerAndInactiveExcluded() throws Exception {
        long learnerId = 100L;

        // 1. Learner themselves offering the skill
        User learner = createUser(learnerId, "Learner", new BigDecimal("5.00"), 5, true);
        OfferedSkill learnerOffer = createOffer(101L, learnerId, 10, 1, 3, true);

        // 2. Inactive user offering the skill
        User inactiveUser = createUser(2L, "Inactive", new BigDecimal("5.00"), 5, false);
        OfferedSkill inactiveUserOffer = createOffer(102L, 2L, 10, 1, 3, true);

        // 3. Inactive listing
        User validUser = createUser(3L, "Active Tutor", new BigDecimal("4.50"), 5, true);
        OfferedSkill inactiveOffer = createOffer(103L, 3L, 10, 1, 3, false);

        userDao.users.put(learnerId, learner);
        userDao.users.put(2L, inactiveUser);
        userDao.users.put(3L, validUser);

        listingDao.offered.add(learnerOffer);
        listingDao.offered.add(inactiveUserOffer);
        listingDao.offered.add(inactiveOffer);

        // All candidates should be excluded -> NoMatchFoundException
        assertThrows(NoMatchFoundException.class, () ->
                weightedEngine.findProviders(learnerId, 10, 10));
    }

    @Test
    public void testTopKOrderingStable() throws Exception {
        long learnerId = 100L;

        // Add 5 candidates with varying scores
        for (int i = 1; i <= 5; i++) {
            long uid = (long) i;
            BigDecimal rating = new BigDecimal("3." + i);
            User u = createUser(uid, "Tutor " + i, rating, 5, true);
            OfferedSkill o = createOffer(100L + i, uid, 10, 1, 3, true);
            userDao.users.put(uid, u);
            listingDao.offered.add(o);
        }

        // Ask for top 3
        List<MatchResult> top3 = weightedEngine.findProviders(learnerId, 10, 3);
        assertEquals(3, top3.size());
        // Must be strictly descending by score
        assertTrue(top3.get(0).getScore() >= top3.get(1).getScore());
        assertTrue(top3.get(1).getScore() >= top3.get(2).getScore());
    }

    @Test
    public void testSwitchingMatchEngineChangesOrdering() throws Exception {
        long learnerId = 100L;

        // Provider 1: rating 5.0, price 5 credits/hr
        User p1 = createUser(1L, "P1", new BigDecimal("5.00"), 5, true);
        OfferedSkill o1 = createOffer(101L, 1L, 10, 1, 5, true);

        // Provider 2: rating 5.0, price 2 credits/hr (cheaper at same rating)
        User p2 = createUser(2L, "P2", new BigDecimal("5.00"), 5, true);
        OfferedSkill o2 = createOffer(102L, 2L, 10, 1, 2, true);

        userDao.users.put(1L, p1);
        userDao.users.put(2L, p2);
        listingDao.offered.add(o1);
        listingDao.offered.add(o2);

        // Factory returns RatingFirstMatchEngine when config is set to "rating"
        Config.set("match.engine", "rating");
        MatchEngine ratingEngine = MatchEngineFactory.getEngine();
        assertInstanceOf(RatingFirstMatchEngine.class, ratingEngine);

        RatingFirstMatchEngine engine = new RatingFirstMatchEngine(listingDao, userDao, skillDao, availabilityDao);
        List<MatchResult> ratingResults = engine.findProviders(learnerId, 10, 10);

        assertEquals(2, ratingResults.size());
        // In rating engine, at equal rating (5.0), lowest price (2 credits) beats higher price (5 credits)
        assertEquals(2L, ratingResults.get(0).getProvider().getId());
        assertEquals(1L, ratingResults.get(1).getProvider().getId());
    }

    private User createUser(long id, String name, BigDecimal rating, int ratingCount, boolean active) {
        Student s = new Student();
        s.setId(id);
        s.setFullName(name);
        s.setEmail(name.toLowerCase().replace(" ", "") + "@skillswap.local");
        s.setAvgRating(rating);
        s.setRatingCount(ratingCount);
        s.setActive(active);
        s.setRole(Role.STUDENT);
        return s;
    }

    private OfferedSkill createOffer(long id, long userId, int skillId, int categoryId, int rate, boolean active) {
        OfferedSkill o = new OfferedSkill();
        o.setId(id);
        o.setUserId(userId);
        o.setSkillId(skillId);
        o.setCategoryId(categoryId);
        o.setCreditsPerHour(rate);
        o.setLevel(SkillLevel.INTERMEDIATE);
        o.setActive(active);
        return o;
    }

    // Mock DAOs for in-memory unit testing
    private static class MockUserDao extends com.skillswap.dao.jdbc.JdbcUserDao {
        final Map<Long, User> users = new HashMap<>();

        @Override
        public Optional<User> findById(long id) {
            return Optional.ofNullable(users.get(id));
        }
    }

    private static class MockSkillDao extends com.skillswap.dao.jdbc.JdbcSkillDao {
        final Map<Integer, Skill> skills = new HashMap<>();

        @Override
        public Optional<Skill> findById(int id) {
            return Optional.ofNullable(skills.get(id));
        }

        @Override
        public List<Skill> findAll() {
            return new ArrayList<>(skills.values());
        }
    }

    private static class MockAvailabilityDao extends com.skillswap.dao.jdbc.JdbcAvailabilityDao {
        final Map<Long, List<AvailabilitySlot>> slots = new HashMap<>();

        @Override
        public List<AvailabilitySlot> findByUser(long userId) {
            return slots.getOrDefault(userId, List.of());
        }
    }

    private static class MockListingDao extends com.skillswap.dao.jdbc.JdbcListingDao {
        final List<OfferedSkill> offered = new ArrayList<>();

        @Override
        public List<OfferedSkill> findOfferedBySkill(int skillId) {
            List<OfferedSkill> res = new ArrayList<>();
            for (OfferedSkill o : offered) {
                if (o.getSkillId() == skillId && o.isActive()) {
                    res.add(o);
                }
            }
            return res;
        }

        @Override
        public List<OfferedSkill> findOfferedByCategory(int categoryId) {
            List<OfferedSkill> res = new ArrayList<>();
            for (OfferedSkill o : offered) {
                if (o.getCategoryId() == categoryId && o.isActive()) {
                    res.add(o);
                }
            }
            return res;
        }
    }
}
