package com.skillswap.scheduler;

import com.skillswap.dao.ListingDao;
import com.skillswap.dao.UserDao;
import com.skillswap.dao.jdbc.JdbcListingDao;
import com.skillswap.dao.jdbc.JdbcUserDao;
import com.skillswap.db.Database;
import com.skillswap.model.OfferedSkill;
import com.skillswap.model.User;
import com.skillswap.model.enums.NotificationType;
import com.skillswap.service.NotificationService;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Scheduled background task scanning for newly offered skills and notifying matching learners (F-25).
 */
public class MatchScanTask implements Runnable {

    private static final Logger LOGGER = Logger.getLogger(MatchScanTask.class.getName());

    private final ListingDao listingDao;
    private final UserDao userDao;
    private final NotificationService notificationService;
    private LocalDateTime lastScanTime;

    /**
     * Default constructor wiring standard services.
     */
    public MatchScanTask() {
        this(new JdbcListingDao(), new JdbcUserDao(), NotificationService.getInstance());
    }

    /**
     * Constructs MatchScanTask with injected dependencies.
     *
     * @param listingDao DAO for listings
     * @param userDao DAO for users
     * @param notificationService service for dispatching notifications
     */
    public MatchScanTask(ListingDao listingDao, UserDao userDao, NotificationService notificationService) {
        this.listingDao = listingDao;
        this.userDao = userDao;
        this.notificationService = notificationService;
        this.lastScanTime = LocalDateTime.now().minusMinutes(5);
    }

    @Override
    public void run() {
        try {
            LocalDateTime now = LocalDateTime.now();
            List<OfferedSkill> newOffers = listingDao.findOfferedCreatedSince(lastScanTime);
            lastScanTime = now;

            if (newOffers.isEmpty()) {
                return;
            }

            for (OfferedSkill offer : newOffers) {
                User provider = userDao.findById(offer.getUserId()).orElse(null);
                String providerName = provider != null ? provider.getFullName() : "A campus peer";
                String skillName = offer.getSkillName() != null ? offer.getSkillName() : "a skill";

                List<Long> interestedLearnerIds = findLearnerIdsWantingSkill(offer.getSkillId(), offer.getUserId());
                for (Long learnerId : interestedLearnerIds) {
                    notificationService.notify(
                            learnerId,
                            NotificationType.NEW_MATCH,
                            "New Match Available!",
                            providerName + " is now offering to teach " + skillName + ".",
                            "/browse?skillId=" + offer.getSkillId()
                    );
                }
            }
        } catch (Throwable t) {
            LOGGER.log(Level.WARNING, "Error executing MatchScanTask", t);
        }
    }

    private List<Long> findLearnerIdsWantingSkill(int skillId, long providerId) {
        String sql = "SELECT us.user_id FROM user_skills us " +
                "JOIN users u ON us.user_id = u.id " +
                "WHERE us.skill_id = ? AND us.type = 'WANTED' AND us.is_active = TRUE AND u.is_active = TRUE AND us.user_id <> ?";
        List<Long> list = new ArrayList<>();
        try (Connection conn = Database.acquire();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, skillId);
            ps.setLong(2, providerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(rs.getLong(1));
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to query learners wanting skill " + skillId, e);
        }
        return list;
    }

    public void setLastScanTime(LocalDateTime lastScanTime) {
        this.lastScanTime = lastScanTime;
    }

    public LocalDateTime getLastScanTime() {
        return lastScanTime;
    }
}
