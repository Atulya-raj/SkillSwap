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
import com.skillswap.dao.ReviewDao;
import com.skillswap.dao.UserDao;
import com.skillswap.dao.jdbc.JdbcBadgeDao;
import com.skillswap.dao.jdbc.JdbcListingDao;
import com.skillswap.dao.jdbc.JdbcReviewDao;
import com.skillswap.dao.jdbc.JdbcUserDao;
import com.skillswap.model.Badge;
import com.skillswap.model.User;

import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service that evaluates badge rules and awards badges to qualified users.
 */
public class BadgeService {

    private static final Logger LOGGER = Logger.getLogger(BadgeService.class.getName());
    private static final BadgeService INSTANCE = new BadgeService();

    private final UserDao userDao;
    private final ListingDao listingDao;
    private final ReviewDao reviewDao;
    private final BadgeDao badgeDao;
    private final List<BadgeRule> rules;

    public BadgeService() {
        this(new JdbcUserDao(), new JdbcListingDao(), new JdbcReviewDao(), new JdbcBadgeDao(),
                List.of(
                        new FirstSwapRule(),
                        new Mentor10Rule(),
                        new EternalLearnerRule(),
                        new PerfectFiveRule(),
                        new ConnectorRule(),
                        new ReliableRule()
                ));
    }

    public BadgeService(UserDao userDao, ListingDao listingDao, ReviewDao reviewDao,
                        BadgeDao badgeDao, List<BadgeRule> rules) {
        this.userDao = userDao;
        this.listingDao = listingDao;
        this.reviewDao = reviewDao;
        this.badgeDao = badgeDao;
        this.rules = rules;
    }

    /**
     * Returns the singleton instance of BadgeService.
     *
     * @return badge service singleton
     */
    public static BadgeService getInstance() {
        return INSTANCE;
    }

    /**
     * Evaluates all badge rules for a user and awards any qualifying badges not already held.
     *
     * @param userId user id
     */
    public void evaluate(long userId) {
        try {
            Optional<User> userOpt = userDao.findById(userId);
            if (userOpt.isEmpty()) {
                return;
            }
            User user = userOpt.get();

            int fiveStar = reviewDao.countFiveStar(userId);
            int offeredCount = listingDao.countOfferedByUser(userId);

            BadgeStats stats = new BadgeStats(
                    user.getSessionsTaught(),
                    user.getSessionsLearned(),
                    user.getAvgRating(),
                    fiveStar,
                    offeredCount,
                    user.getReliabilityStrikes()
            );

            for (BadgeRule rule : rules) {
                if (!badgeDao.has(userId, rule.code()) && rule.qualifies(user, stats)) {
                    badgeDao.award(userId, rule.code());
                    LOGGER.info("Awarded badge " + rule.code() + " to user id " + userId);
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error evaluating badges for user " + userId, e);
        }
    }

    /**
     * Returns all badges available in the system.
     *
     * @return list of all badges
     */
    public List<Badge> findAll() {
        return badgeDao.findAll();
    }

    /**
     * Returns all badges earned by a user.
     *
     * @param userId user id
     * @return list of earned badges
     */
    public List<Badge> findForUser(long userId) {
        return badgeDao.findForUser(userId);
    }
}
