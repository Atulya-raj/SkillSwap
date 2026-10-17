package com.skillswap.service;

import com.skillswap.dao.ReviewDao;
import com.skillswap.dao.SessionRequestDao;
import com.skillswap.dao.UserDao;
import com.skillswap.dao.jdbc.JdbcReviewDao;
import com.skillswap.dao.jdbc.JdbcSessionRequestDao;
import com.skillswap.dao.jdbc.JdbcUserDao;
import com.skillswap.db.TransactionManager;
import com.skillswap.exception.AuthorizationException;
import com.skillswap.exception.ResourceNotFoundException;
import com.skillswap.exception.ValidationException;
import com.skillswap.model.Review;
import com.skillswap.model.SessionRequest;
import com.skillswap.model.enums.NotificationType;
import com.skillswap.model.enums.SessionStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Service for submitting, moderating, and querying session reviews and ratings.
 */
public class ReviewService {

    private static volatile ReviewService instance;

    private final ReviewDao reviewDao;
    private final SessionRequestDao sessionRequestDao;
    private final UserDao userDao;
    private final BadgeService badgeService;
    private final NotificationService notificationService;

    /**
     * Default constructor wiring standard DAOs and singletons.
     */
    public ReviewService() {
        this(new JdbcReviewDao(), new JdbcSessionRequestDao(), new JdbcUserDao(),
                BadgeService.getInstance(), NotificationService.getInstance());
    }

    /**
     * Constructs ReviewService with injected dependencies.
     *
     * @param reviewDao review DAO
     * @param sessionRequestDao session request DAO
     * @param userDao user DAO
     * @param badgeService badge evaluation service
     * @param notificationService notification service
     */
    public ReviewService(ReviewDao reviewDao, SessionRequestDao sessionRequestDao, UserDao userDao,
                         BadgeService badgeService, NotificationService notificationService) {
        this.reviewDao = reviewDao;
        this.sessionRequestDao = sessionRequestDao;
        this.userDao = userDao;
        this.badgeService = badgeService;
        this.notificationService = notificationService;
    }

    /**
     * Returns singleton instance.
     *
     * @return global ReviewService instance
     */
    public static ReviewService getInstance() {
        if (instance == null) {
            synchronized (ReviewService.class) {
                if (instance == null) {
                    instance = new ReviewService();
                }
            }
        }
        return instance;
    }

    /**
     * Submits a rating and optional review for a completed swap session.
     *
     * @param reviewerId user leaving review
     * @param sessionId session request ID
     * @param rating 1 to 5 stars
     * @param comment optional written feedback
     * @return persisted Review
     * @throws Exception on validation failure or transaction error
     */
    public Review submit(long reviewerId, long sessionId, int rating, String comment) throws Exception {
        if (rating < 1 || rating > 5) {
            throw new ValidationException("rating", "Rating must be between 1 and 5 stars.");
        }
        if (comment != null && comment.length() > 500) {
            throw new ValidationException("comment", "Comment cannot exceed 500 characters.");
        }

        SessionRequest sr = sessionRequestDao.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session request not found: " + sessionId));

        if (sr.getStatus() != SessionStatus.COMPLETED) {
            throw new ValidationException("status", "Reviews can only be submitted for COMPLETED sessions.");
        }

        if (sr.getLearnerId() != reviewerId && sr.getProviderId() != reviewerId) {
            throw new AuthorizationException("You were not a participant in this session.");
        }

        if (reviewDao.exists(sessionId, reviewerId)) {
            throw new ValidationException("review", "You have already reviewed this session.");
        }

        long revieweeId = (reviewerId == sr.getLearnerId()) ? sr.getProviderId() : sr.getLearnerId();

        Review review = new Review();
        review.setSessionRequestId(sessionId);
        review.setReviewerId(reviewerId);
        review.setRevieweeId(revieweeId);
        review.setRating(rating);
        review.setComment(comment != null ? comment.trim() : "");
        review.setHidden(false);
        review.setCreatedAt(LocalDateTime.now());

        TransactionManager.execute(() -> {
            long id = reviewDao.insert(review);
            review.setId(id);

            BigDecimal avg = reviewDao.avgFor(revieweeId);
            int count = reviewDao.countFor(revieweeId);
            userDao.applyRatingStats(revieweeId, avg, count);
            return review;
        });

        if (badgeService != null) {
            badgeService.evaluate(revieweeId);
        }

        if (notificationService != null) {
            notificationService.notify(
                    revieweeId,
                    NotificationType.NEW_REVIEW,
                    "New Review Received",
                    "You received a " + rating + "-star rating on SkillSwap!",
                    "/users/view?id=" + revieweeId
            );
        }

        return review;
    }

    /**
     * Hides a review and recomputes the subject user's rating stats.
     *
     * @param adminId administrator ID
     * @param reviewId review ID to hide
     * @throws Exception on not found or transaction failure
     */
    public void hide(long adminId, long reviewId) throws Exception {
        Review r = reviewDao.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found: " + reviewId));

        TransactionManager.execute(() -> {
            reviewDao.setHidden(reviewId, true);
            BigDecimal avg = reviewDao.avgFor(r.getRevieweeId());
            int count = reviewDao.countFor(r.getRevieweeId());
            userDao.applyRatingStats(r.getRevieweeId(), avg, count);
            return null;
        });
    }

    /**
     * Unhides a review and recomputes user's rating stats.
     *
     * @param adminId administrator ID
     * @param reviewId review ID to unhide
     * @throws Exception on not found or transaction failure
     */
    public void unhide(long adminId, long reviewId) throws Exception {
        Review r = reviewDao.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found: " + reviewId));

        TransactionManager.execute(() -> {
            reviewDao.setHidden(reviewId, false);
            BigDecimal avg = reviewDao.avgFor(r.getRevieweeId());
            int count = reviewDao.countFor(r.getRevieweeId());
            userDao.applyRatingStats(r.getRevieweeId(), avg, count);
            return null;
        });
    }

    /**
     * Finds visible reviews received by a user.
     *
     * @param userId user ID
     * @param limit maximum records
     * @return list of visible reviews
     */
    public List<Review> findForUser(long userId, int limit) {
        return reviewDao.findForUser(userId, limit);
    }

    /**
     * Checks if a user has already reviewed a session.
     *
     * @param sessionId session request ID
     * @param reviewerId user ID
     * @return true if review exists, false otherwise
     */
    public boolean hasReviewed(long sessionId, long reviewerId) {
        return reviewDao.exists(sessionId, reviewerId);
    }
}
