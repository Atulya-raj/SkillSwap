package com.skillswap.service;

import com.skillswap.dao.AnalyticsDao;
import com.skillswap.dao.BadgeDao;
import com.skillswap.dao.CreditDao;
import com.skillswap.dao.NotificationDao;
import com.skillswap.dao.SessionRequestDao;
import com.skillswap.dao.UserDao;
import com.skillswap.dao.jdbc.JdbcAnalyticsDao;
import com.skillswap.dao.jdbc.JdbcBadgeDao;
import com.skillswap.dao.jdbc.JdbcCreditDao;
import com.skillswap.dao.jdbc.JdbcNotificationDao;
import com.skillswap.dao.jdbc.JdbcSessionRequestDao;
import com.skillswap.dao.jdbc.JdbcUserDao;
import com.skillswap.exception.ResourceNotFoundException;
import com.skillswap.model.MatchResult;
import com.skillswap.model.Badge;
import com.skillswap.model.SessionRequest;
import com.skillswap.model.User;
import com.skillswap.model.enums.LedgerType;
import com.skillswap.model.enums.SessionStatus;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Service aggregating user dashboard statistics and administrative system-wide metrics.
 */
public class AnalyticsService {

    private final UserDao userDao;
    private final SessionRequestDao sessionRequestDao;
    private final CreditDao creditDao;
    private final BadgeDao badgeDao;
    private final NotificationDao notificationDao;
    private final AnalyticsDao analyticsDao;
    private final MatchService matchService;

    /**
     * Default constructor wiring JDBC DAOs and default MatchService.
     */
    public AnalyticsService() {
        this(new JdbcUserDao(),
                new JdbcSessionRequestDao(),
                new JdbcCreditDao(),
                new JdbcBadgeDao(),
                new JdbcNotificationDao(),
                new JdbcAnalyticsDao(),
                new MatchService());
    }

    /**
     * Constructor with dependency injection.
     */
    public AnalyticsService(UserDao userDao,
                            SessionRequestDao sessionRequestDao,
                            CreditDao creditDao,
                            BadgeDao badgeDao,
                            NotificationDao notificationDao,
                            AnalyticsDao analyticsDao,
                            MatchService matchService) {
        this.userDao = userDao;
        this.sessionRequestDao = sessionRequestDao;
        this.creditDao = creditDao;
        this.badgeDao = badgeDao;
        this.notificationDao = notificationDao;
        this.analyticsDao = analyticsDao;
        this.matchService = matchService;
    }

    /**
     * Aggregates complete personal dashboard data for a student user.
     *
     * @param userId user ID
     * @return UserDashboardData aggregated DTO
     * @throws ResourceNotFoundException if user is not found
     */
    public UserDashboardData userDashboard(long userId) throws ResourceNotFoundException {
        User user = userDao.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        int availableCredits = user.getCredits();
        int heldCredits = creditDao.sumHeldCredits(userId);
        int totalEarned = creditDao.sumByType(userId, LedgerType.EARN);

        int totalHold = Math.abs(creditDao.sumByType(userId, LedgerType.HOLD));
        int totalRelease = creditDao.sumByType(userId, LedgerType.RELEASE);
        int totalSpent = Math.max(0, totalHold - totalRelease);

        List<SessionRequest> upcoming = sessionRequestDao.findUpcomingAccepted(userId, 5);
        List<SessionRequest> pendingIncoming = sessionRequestDao.findIncoming(userId, SessionStatus.PENDING, 5, 0);
        int pendingCount = sessionRequestDao.countPendingIncoming(userId);

        List<MatchResult> recommendations = matchService.recommend(userId, 3);
        int unreadNotifications = notificationDao.countUnread(userId);
        List<Badge> badges = badgeDao.findForUser(userId);

        return new UserDashboardData(
                user,
                availableCredits,
                heldCredits,
                totalEarned,
                totalSpent,
                upcoming,
                pendingIncoming,
                pendingCount,
                recommendations,
                unreadNotifications,
                badges
        );
    }

    /**
     * Aggregates administrative system metrics with percentage bar data calculated for CSS rendering.
     *
     * @param activeUsersCount count of currently active HTTP sessions
     * @return AdminAnalyticsData aggregated DTO
     */
    public AdminAnalyticsData adminOverview(int activeUsersCount) {
        int totalUsers = analyticsDao.totalUsers();
        int creditsCirculation = creditDao.totalCreditsInCirculation();
        Map<SessionStatus, Integer> statusMap = analyticsDao.sessionsByStatus();

        int totalSessions = 0;
        for (int count : statusMap.values()) {
            totalSessions += count;
        }

        int completed = statusMap.getOrDefault(SessionStatus.COMPLETED, 0);
        double completionRate = totalSessions > 0 ? (completed * 100.0) / totalSessions : 0.0;

        // Top 10 Wanted skills with percentage widths
        List<AnalyticsDao.SkillCount> rawWanted = analyticsDao.topWanted(10);
        int maxWanted = 1;
        for (AnalyticsDao.SkillCount sc : rawWanted) {
            if (sc.getCount() > maxWanted) {
                maxWanted = sc.getCount();
            }
        }
        List<SkillDemandBar> topWantedBars = new ArrayList<>();
        for (AnalyticsDao.SkillCount sc : rawWanted) {
            int pct = (int) Math.round((sc.getCount() * 100.0) / maxWanted);
            topWantedBars.add(new SkillDemandBar(sc.getSkillName(), sc.getCount(), Math.max(4, pct)));
        }

        // Top 10 Offered skills with percentage widths
        List<AnalyticsDao.SkillCount> rawOffered = analyticsDao.topOffered(10);
        int maxOffered = 1;
        for (AnalyticsDao.SkillCount sc : rawOffered) {
            if (sc.getCount() > maxOffered) {
                maxOffered = sc.getCount();
            }
        }
        List<SkillDemandBar> topOfferedBars = new ArrayList<>();
        for (AnalyticsDao.SkillCount sc : rawOffered) {
            int pct = (int) Math.round((sc.getCount() * 100.0) / maxOffered);
            topOfferedBars.add(new SkillDemandBar(sc.getSkillName(), sc.getCount(), Math.max(4, pct)));
        }

        // New users per week (last 8 weeks) with percentage heights
        List<AnalyticsDao.WeekCount> rawWeeks = analyticsDao.newUsersPerWeek(8);
        int maxWeek = 1;
        for (AnalyticsDao.WeekCount wc : rawWeeks) {
            if (wc.getCount() > maxWeek) {
                maxWeek = wc.getCount();
            }
        }
        List<WeeklyUserBar> weeklyBars = new ArrayList<>();
        for (AnalyticsDao.WeekCount wc : rawWeeks) {
            int pct = (int) Math.round((wc.getCount() * 100.0) / maxWeek);
            weeklyBars.add(new WeeklyUserBar(wc.getWeekLabel(), wc.getCount(), Math.max(6, pct)));
        }

        return new AdminAnalyticsData(
                totalUsers,
                activeUsersCount,
                creditsCirculation,
                totalSessions,
                completed,
                completionRate,
                statusMap,
                topWantedBars,
                topOfferedBars,
                weeklyBars
        );
    }

    /**
     * DTO containing all personal metrics and widgets for the student dashboard.
     */
    public static class UserDashboardData {
        private final User user;
        private final int availableCredits;
        private final int heldCredits;
        private final int totalEarned;
        private final int totalSpent;
        private final List<SessionRequest> upcomingSessions;
        private final List<SessionRequest> pendingIncoming;
        private final int pendingIncomingCount;
        private final List<MatchResult> recommendations;
        private final int unreadNotifications;
        private final List<Badge> badges;

        public UserDashboardData(User user,
                                 int availableCredits,
                                 int heldCredits,
                                 int totalEarned,
                                 int totalSpent,
                                 List<SessionRequest> upcomingSessions,
                                 List<SessionRequest> pendingIncoming,
                                 int pendingIncomingCount,
                                 List<MatchResult> recommendations,
                                 int unreadNotifications,
                                 List<Badge> badges) {
            this.user = user;
            this.availableCredits = availableCredits;
            this.heldCredits = heldCredits;
            this.totalEarned = totalEarned;
            this.totalSpent = totalSpent;
            this.upcomingSessions = upcomingSessions;
            this.pendingIncoming = pendingIncoming;
            this.pendingIncomingCount = pendingIncomingCount;
            this.recommendations = recommendations;
            this.unreadNotifications = unreadNotifications;
            this.badges = badges;
        }

        public User getUser() { return user; }
        public int getAvailableCredits() { return availableCredits; }
        public int getHeldCredits() { return heldCredits; }
        public int getTotalEarned() { return totalEarned; }
        public int getTotalSpent() { return totalSpent; }
        public List<SessionRequest> getUpcomingSessions() { return upcomingSessions; }
        public List<SessionRequest> getPendingIncoming() { return pendingIncoming; }
        public int getPendingIncomingCount() { return pendingIncomingCount; }
        public List<MatchResult> getRecommendations() { return recommendations; }
        public int getUnreadNotifications() { return unreadNotifications; }
        public List<Badge> getBadges() { return badges; }
        public BigDecimal getAvgRating() { return user.getAvgRating(); }
        public int getRatingCount() { return user.getRatingCount(); }
        public int getSessionsTaught() { return user.getSessionsTaught(); }
        public int getSessionsLearned() { return user.getSessionsLearned(); }
        public int getReliabilityStrikes() { return user.getReliabilityStrikes(); }
    }

    /**
     * DTO for a skill frequency metric bar with CSS width percentage.
     */
    public static class SkillDemandBar {
        private final String skillName;
        private final int count;
        private final int percentWidth;

        public SkillDemandBar(String skillName, int count, int percentWidth) {
            this.skillName = skillName;
            this.count = count;
            this.percentWidth = percentWidth;
        }

        public String getSkillName() { return skillName; }
        public int getCount() { return count; }
        public int getPercentWidth() { return percentWidth; }
    }

    /**
     * DTO for weekly registration volume with CSS height/width percentage.
     */
    public static class WeeklyUserBar {
        private final String weekLabel;
        private final int count;
        private final int percent;

        public WeeklyUserBar(String weekLabel, int count, int percent) {
            this.weekLabel = weekLabel;
            this.count = count;
            this.percent = percent;
        }

        public String getWeekLabel() { return weekLabel; }
        public int getCount() { return count; }
        public int getPercent() { return percent; }
    }

    /**
     * DTO containing all global platform metrics for the admin overview.
     */
    public static class AdminAnalyticsData {
        private final int totalUsers;
        private final int activeUsersOnline;
        private final int totalCreditsInCirculation;
        private final int totalSessions;
        private final int completedSessions;
        private final double completionRate;
        private final Map<SessionStatus, Integer> sessionsByStatus;
        private final List<SkillDemandBar> topWanted;
        private final List<SkillDemandBar> topOffered;
        private final List<WeeklyUserBar> weeklyRegistrations;

        public AdminAnalyticsData(int totalUsers,
                                  int activeUsersOnline,
                                  int totalCreditsInCirculation,
                                  int totalSessions,
                                  int completedSessions,
                                  double completionRate,
                                  Map<SessionStatus, Integer> sessionsByStatus,
                                  List<SkillDemandBar> topWanted,
                                  List<SkillDemandBar> topOffered,
                                  List<WeeklyUserBar> weeklyRegistrations) {
            this.totalUsers = totalUsers;
            this.activeUsersOnline = activeUsersOnline;
            this.totalCreditsInCirculation = totalCreditsInCirculation;
            this.totalSessions = totalSessions;
            this.completedSessions = completedSessions;
            this.completionRate = completionRate;
            this.sessionsByStatus = sessionsByStatus;
            this.topWanted = topWanted;
            this.topOffered = topOffered;
            this.weeklyRegistrations = weeklyRegistrations;
        }

        public int getTotalUsers() { return totalUsers; }
        public int getActiveUsersOnline() { return activeUsersOnline; }
        public int getTotalCreditsInCirculation() { return totalCreditsInCirculation; }
        public int getTotalSessions() { return totalSessions; }
        public int getCompletedSessions() { return completedSessions; }
        public double getCompletionRate() { return completionRate; }
        public String getCompletionRateFormatted() { return String.format("%.1f", completionRate); }
        public Map<SessionStatus, Integer> getSessionsByStatus() { return sessionsByStatus; }
        public List<SkillDemandBar> getTopWanted() { return topWanted; }
        public List<SkillDemandBar> getTopOffered() { return topOffered; }
        public List<WeeklyUserBar> getWeeklyRegistrations() { return weeklyRegistrations; }
    }
}
