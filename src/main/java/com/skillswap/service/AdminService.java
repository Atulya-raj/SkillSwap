package com.skillswap.service;

import com.skillswap.dao.ReportDao;
import com.skillswap.dao.UserDao;
import com.skillswap.dao.jdbc.JdbcReportDao;
import com.skillswap.dao.jdbc.JdbcUserDao;
import com.skillswap.exception.ResourceNotFoundException;
import com.skillswap.model.Report;
import com.skillswap.model.User;
import com.skillswap.model.enums.ReportStatus;

import java.util.List;

/**
 * Service managing administrative operations including user moderation, credit adjustments, and report resolution.
 */
public class AdminService {

    private final UserDao userDao;
    private final ReportDao reportDao;
    private final CreditService creditService;
    private final ReviewService reviewService;

    /**
     * Default constructor wiring standard DAOs and dependent services.
     */
    public AdminService() {
        this(new JdbcUserDao(), new JdbcReportDao(), new CreditService(), new ReviewService());
    }

    /**
     * Constructs AdminService with injected dependencies.
     */
    public AdminService(UserDao userDao, ReportDao reportDao, CreditService creditService, ReviewService reviewService) {
        this.userDao = userDao;
        this.reportDao = reportDao;
        this.creditService = creditService;
        this.reviewService = reviewService;
    }

    /**
     * Searches or lists users with query, limit, and offset.
     *
     * @param query search keyword or null/empty for all
     * @param limit page size
     * @param offset page offset
     * @return list of matching users
     */
    public List<User> listUsers(String query, int limit, int offset) {
        if (query != null && !query.trim().isEmpty()) {
            return userDao.search(query.trim(), limit, offset);
        }
        return userDao.findAll(limit, offset);
    }

    /**
     * Counts users matching a search query.
     *
     * @param query search keyword or null/empty for all
     * @return total count
     */
    public int countUsers(String query) {
        return userDao.count(query != null ? query.trim() : null);
    }

    /**
     * Toggles active status of a user (deactivated users cannot log in).
     *
     * @param userId user ID
     * @param active desired active state
     * @throws ResourceNotFoundException if user does not exist
     */
    public void toggleActive(long userId, boolean active) throws ResourceNotFoundException {
        userDao.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        userDao.setActive(userId, active);
    }

    /**
     * Performs a manual credit adjustment with an audit ledger entry and required reason.
     *
     * @param adminId administrator performing the adjustment
     * @param userId target user ID
     * @param amount signed credit change
     * @param reason required explanatory reason
     * @throws Exception on validation or database failure
     */
    public void adjustCredits(long adminId, long userId, int amount, String reason) throws Exception {
        creditService.adminAdjust(userId, amount, reason);
    }

    /**
     * Lists moderation reports filtered by status.
     *
     * @param status report status filter (OPEN, RESOLVED, DISMISSED)
     * @param limit page limit
     * @param offset page offset
     * @return list of matching reports
     */
    public List<Report> listReports(ReportStatus status, int limit, int offset) {
        return reportDao.findByStatus(status != null ? status : ReportStatus.OPEN, limit, offset);
    }

    /**
     * Resolves or dismisses a moderation report.
     *
     * @param reportId report ID
     * @param adminId administrator ID
     * @param resolution final status (RESOLVED or DISMISSED)
     */
    public void resolveReport(long reportId, long adminId, ReportStatus resolution) {
        reportDao.resolve(reportId, adminId, resolution);
    }

    /**
     * Hides an abusive review and recomputes the recipient's average rating.
     *
     * @param adminId administrator ID
     * @param reviewId review ID
     * @throws Exception on failure
     */
    public void hideReview(long adminId, long reviewId) throws Exception {
        reviewService.hide(adminId, reviewId);
    }

    /**
     * Unhides a previously hidden review and recomputes rating stats.
     *
     * @param adminId administrator ID
     * @param reviewId review ID
     * @throws Exception on failure
     */
    public void unhideReview(long adminId, long reviewId) throws Exception {
        reviewService.unhide(adminId, reviewId);
    }
}
