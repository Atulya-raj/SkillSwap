package com.skillswap.service;

import com.skillswap.dao.ListingDao;
import com.skillswap.dao.SessionRequestDao;
import com.skillswap.dao.UserDao;
import com.skillswap.dao.jdbc.JdbcListingDao;
import com.skillswap.dao.jdbc.JdbcSessionRequestDao;
import com.skillswap.dao.jdbc.JdbcUserDao;
import com.skillswap.db.TransactionManager;
import com.skillswap.exception.AuthorizationException;
import com.skillswap.exception.DuplicateRequestException;
import com.skillswap.exception.InsufficientCreditsException;
import com.skillswap.exception.InvalidStateTransitionException;
import com.skillswap.exception.ResourceNotFoundException;
import com.skillswap.exception.SchedulingConflictException;
import com.skillswap.exception.SkillSwapException;
import com.skillswap.exception.ValidationException;
import com.skillswap.model.OfferedSkill;
import com.skillswap.model.SessionRequest;
import com.skillswap.model.SkillListing;
import com.skillswap.model.User;
import com.skillswap.model.enums.ListingType;
import com.skillswap.model.enums.NotificationType;
import com.skillswap.model.enums.SessionMode;
import com.skillswap.model.enums.SessionStatus;
import com.skillswap.util.Config;
import com.skillswap.util.Validator;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service orchestrating peer session requests, escrow transactions, lifecycle state machine, and confirmations.
 */
public class SessionService {

    private static final Logger LOGGER = Logger.getLogger(SessionService.class.getName());

    private final SessionRequestDao sessionRequestDao;
    private final ListingDao listingDao;
    private final UserDao userDao;
    private final CreditService creditService;
    private final BadgeService badgeService;
    private final DraftService draftService;
    private final NotificationService notificationService;

    /**
     * Default constructor wiring standard implementations.
     */
    public SessionService() {
        this(new JdbcSessionRequestDao(), new JdbcListingDao(), new JdbcUserDao(),
                new CreditService(), new BadgeService(), new DraftService(), NotificationService.getInstance());
    }

    /**
     * Constructs SessionService with injected dependencies without explicit NotificationService.
     */
    public SessionService(SessionRequestDao sessionRequestDao, ListingDao listingDao, UserDao userDao,
                          CreditService creditService, BadgeService badgeService, DraftService draftService) {
        this(sessionRequestDao, listingDao, userDao, creditService, badgeService, draftService, NotificationService.getInstance());
    }

    /**
     * Constructs SessionService with all injected dependencies including NotificationService.
     */
    public SessionService(SessionRequestDao sessionRequestDao, ListingDao listingDao, UserDao userDao,
                          CreditService creditService, BadgeService badgeService, DraftService draftService,
                          NotificationService notificationService) {
        this.sessionRequestDao = sessionRequestDao;
        this.listingDao = listingDao;
        this.userDao = userDao;
        this.creditService = creditService;
        this.badgeService = badgeService;
        this.draftService = draftService;
        this.notificationService = notificationService;
    }

    /**
     * Creates a new session request, holds credits in escrow, and cleans any saved draft.
     *
     * @param learnerId learner user ID
     * @param listingId ID of provider's offered skill listing
     * @param proposedStart scheduled start date and time
     * @param durationMinutes session duration in minutes
     * @param mode online or in-person mode
     * @param meetingDetails online link or physical meeting place
     * @param message introductory message or note
     * @return newly created SessionRequest
     * @throws SkillSwapException if validation fails or credit/duplicate rules are breached
     */
    public SessionRequest create(long learnerId, long listingId, LocalDateTime proposedStart,
                                 int durationMinutes, SessionMode mode, String meetingDetails, String message)
            throws SkillSwapException {
        Validator.validateSessionRequest(proposedStart, durationMinutes, mode, meetingDetails, message);

        SkillListing listing = listingDao.findById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Offered skill listing not found: " + listingId));

        if (!listing.isActive() || listing.getType() != ListingType.OFFERED) {
            throw new ValidationException(Map.of("listing", "Selected listing is inactive or not offered"));
        }

        long providerId = listing.getUserId();
        if (providerId == learnerId) {
            throw new ValidationException(Map.of("learner", "You cannot request a session with yourself"));
        }

        User learner = userDao.findById(learnerId)
                .orElseThrow(() -> new ResourceNotFoundException("Learner user not found: " + learnerId));
        if (!learner.isActive()) {
            throw new ValidationException(Map.of("learner", "Your account is currently inactive"));
        }

        User provider = userDao.findById(providerId)
                .orElseThrow(() -> new ResourceNotFoundException("Provider user not found: " + providerId));
        if (!provider.isActive()) {
            throw new ValidationException(Map.of("provider", "Provider account is currently inactive"));
        }

        if (sessionRequestDao.existsActive(learnerId, providerId, listing.getSkillId())) {
            throw new DuplicateRequestException("You already have an active request for this skill with this provider");
        }

        int creditsPerHour = (listing instanceof OfferedSkill) ? ((OfferedSkill) listing).getCreditsPerHour() : 1;
        int costCredits = (int) Math.ceil((double) (creditsPerHour * durationMinutes) / 60.0);

        SessionRequest created = TransactionManager.execute(() -> {
            creditService.lockUsersInOrder(learnerId, providerId);
            User lockedLearner = userDao.findById(learnerId)
                    .orElseThrow(() -> new ResourceNotFoundException("Learner not found: " + learnerId));
            if (lockedLearner.getCredits() < costCredits) {
                throw new InsufficientCreditsException("Insufficient credits: balance is "
                        + lockedLearner.getCredits() + ", cost is " + costCredits);
            }

            SessionRequest req = new SessionRequest();
            req.setLearnerId(learnerId);
            req.setProviderId(providerId);
            req.setSkillId(listing.getSkillId());
            req.setListingId(listingId);
            req.setProposedStart(proposedStart);
            req.setDurationMinutes(durationMinutes);
            req.setMode(mode);
            req.setMeetingDetails(meetingDetails.trim());
            req.setMessage(message != null ? message.trim() : null);
            req.setCostCredits(costCredits);
            req.setStatus(SessionStatus.PENDING);
            req.setLearnerConfirmed(false);
            req.setProviderConfirmed(false);
            req.setReminderSent(false);
            req.setCreatedAt(LocalDateTime.now());

            long reqId = sessionRequestDao.insert(req);
            req.setId(reqId);

            creditService.hold(learnerId, reqId, costCredits);
            return req;
        });

        draftService.delete(learnerId, listingId);
        if (notificationService != null) {
            notificationService.notify(providerId, NotificationType.REQUEST_RECEIVED,
                    "New Swap Request", learner.getFullName() + " sent you a session request for " + listing.getSkillName(),
                    "/requests/view?id=" + created.getId());
        }
        return created;
    }

    /**
     * Executes guarded state transition on a locked session request.
     */
    private SessionRequest transition(long requestId, SessionStatus expectedFrom, SessionStatus targetTo)
            throws SkillSwapException {
        if (!expectedFrom.canMoveTo(targetTo)) {
            throw new InvalidStateTransitionException(expectedFrom, targetTo);
        }

        SessionRequest sr = sessionRequestDao.lockById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Session request not found: " + requestId));

        if (sr.getStatus() != expectedFrom) {
            throw new InvalidStateTransitionException("Expected status " + expectedFrom + " but current status is " + sr.getStatus());
        }

        boolean updated = sessionRequestDao.updateStatus(requestId, expectedFrom, targetTo);
        if (!updated) {
            throw new InvalidStateTransitionException("Request status was updated concurrently for #" + requestId);
        }

        sr.setStatus(targetTo);
        return sr;
    }

    /**
     * Accepts a pending session request as the provider.
     *
     * @param providerId provider user ID
     * @param requestId session request ID
     * @return updated SessionRequest
     * @throws SkillSwapException if unauthorized or conflict occurs
     */
    public SessionRequest accept(long providerId, long requestId) throws SkillSwapException {
        return TransactionManager.execute(() -> {
            SessionRequest sr = sessionRequestDao.lockById(requestId)
                    .orElseThrow(() -> new ResourceNotFoundException("Session request not found: " + requestId));

            if (sr.getProviderId() != providerId) {
                throw new AuthorizationException("Only the designated provider can accept this request");
            }

            if (sessionRequestDao.hasOverlappingAccepted(providerId, sr.getProposedStart(), sr.getDurationMinutes(), requestId)) {
                throw new SchedulingConflictException("You have an overlapping accepted session during this time window");
            }

            transition(requestId, SessionStatus.PENDING, SessionStatus.ACCEPTED);
            sessionRequestDao.markResponded(requestId);
            sr.setStatus(SessionStatus.ACCEPTED);
            sr.setRespondedAt(LocalDateTime.now());

            if (notificationService != null) {
                notificationService.notify(sr.getLearnerId(), NotificationType.REQUEST_ACCEPTED,
                        "Request Accepted!", "Your session request for " + sr.getSkillName() + " was accepted.",
                        "/requests/view?id=" + requestId);
            }
            return sr;
        });
    }

    /**
     * Rejects a pending session request as the provider and releases held credits to learner.
     *
     * @param providerId provider user ID
     * @param requestId session request ID
     * @return updated SessionRequest
     * @throws SkillSwapException if unauthorized or state transition fails
     */
    public SessionRequest reject(long providerId, long requestId) throws SkillSwapException {
        return TransactionManager.execute(() -> {
            SessionRequest sr = sessionRequestDao.lockById(requestId)
                    .orElseThrow(() -> new ResourceNotFoundException("Session request not found: " + requestId));

            if (sr.getProviderId() != providerId) {
                throw new AuthorizationException("Only the designated provider can reject this request");
            }

            transition(requestId, SessionStatus.PENDING, SessionStatus.REJECTED);
            sessionRequestDao.markResponded(requestId);

            creditService.release(sr.getLearnerId(), requestId, sr.getCostCredits(), "Request rejected by provider");
            sr.setStatus(SessionStatus.REJECTED);
            sr.setRespondedAt(LocalDateTime.now());

            if (notificationService != null) {
                notificationService.notify(sr.getLearnerId(), NotificationType.REQUEST_REJECTED,
                        "Request Declined", "Your session request for " + sr.getSkillName() + " was declined. Held credits released.",
                        "/requests/view?id=" + requestId);
            }
            return sr;
        });
    }

    /**
     * Cancels a pending or accepted session request per the cancellation credit rules.
     *
     * @param userId cancelling user ID (learner or provider)
     * @param requestId session request ID
     * @return updated SessionRequest
     * @throws SkillSwapException if unauthorized or state transition fails
     */
    public SessionRequest cancel(long userId, long requestId) throws SkillSwapException {
        return TransactionManager.execute(() -> {
            SessionRequest sr = sessionRequestDao.lockById(requestId)
                    .orElseThrow(() -> new ResourceNotFoundException("Session request not found: " + requestId));

            if (userId != sr.getLearnerId() && userId != sr.getProviderId()) {
                throw new AuthorizationException("You are not a participant of this session request");
            }

            if (sr.getStatus() == SessionStatus.PENDING) {
                if (userId != sr.getLearnerId()) {
                    throw new AuthorizationException("Only the learner can cancel a pending request (providers can reject)");
                }
                transition(requestId, SessionStatus.PENDING, SessionStatus.CANCELLED);
                creditService.release(sr.getLearnerId(), requestId, sr.getCostCredits(), "Pending request cancelled by learner");
                sr.setStatus(SessionStatus.CANCELLED);

                if (notificationService != null) {
                    notificationService.notify(sr.getProviderId(), NotificationType.REQUEST_CANCELLED,
                            "Request Cancelled", "The pending request for " + sr.getSkillName() + " was cancelled by the learner.",
                            "/requests/view?id=" + requestId);
                }
                return sr;
            }

            if (sr.getStatus() == SessionStatus.ACCEPTED) {
                transition(requestId, SessionStatus.ACCEPTED, SessionStatus.CANCELLED);
                sr.setStatus(SessionStatus.CANCELLED);

                if (userId == sr.getProviderId()) {
                    // Provider cancelled accepted session: full refund to learner
                    creditService.release(sr.getLearnerId(), requestId, sr.getCostCredits(), "Accepted session cancelled by provider");
                    if (notificationService != null) {
                        notificationService.notify(sr.getLearnerId(), NotificationType.REQUEST_CANCELLED,
                                "Session Cancelled", "Your accepted session for " + sr.getSkillName() + " was cancelled by your tutor. Credits refunded.",
                                "/requests/view?id=" + requestId);
                    }
                } else {
                    // Learner cancelled accepted session
                    Duration untilStart = Duration.between(LocalDateTime.now(), sr.getProposedStart());
                    if (untilStart.toMinutes() >= 120) {
                        // 2+ hours before start: full refund
                        creditService.release(sr.getLearnerId(), requestId, sr.getCostCredits(), "Session cancelled 2+ hours before start");
                    } else {
                        // Less than 2 hours before start: 50% refund, remainder to provider
                        int refund = sr.getCostCredits() / 2;
                        int compensation = sr.getCostCredits() - refund;

                        creditService.lockUsersInOrder(sr.getLearnerId(), sr.getProviderId());
                        if (refund > 0) {
                            creditService.release(sr.getLearnerId(), requestId, refund, "Late cancellation refund (50%)");
                        }
                        if (compensation > 0) {
                            creditService.pay(sr.getProviderId(), requestId, compensation, "Late cancellation compensation");
                        }
                    }
                    if (notificationService != null) {
                        notificationService.notify(sr.getProviderId(), NotificationType.REQUEST_CANCELLED,
                                "Session Cancelled", "Your accepted session for " + sr.getSkillName() + " was cancelled by the learner.",
                                "/requests/view?id=" + requestId);
                    }
                }
                return sr;
            }

            throw new InvalidStateTransitionException("Cannot cancel session request in status " + sr.getStatus());
        });
    }

    /**
     * Confirms session completion. When both parties confirm, pays provider and evaluates badges.
     *
     * @param userId confirming user ID
     * @param requestId session request ID
     * @return updated SessionRequest
     * @throws SkillSwapException if session is not accepted, before start time, or unauthorized
     */
    public SessionRequest confirm(long userId, long requestId) throws SkillSwapException {
        return TransactionManager.execute(() -> {
            SessionRequest sr = sessionRequestDao.lockById(requestId)
                    .orElseThrow(() -> new ResourceNotFoundException("Session request not found: " + requestId));

            if (userId != sr.getLearnerId() && userId != sr.getProviderId()) {
                throw new AuthorizationException("You are not a participant of this session");
            }

            if (sr.getStatus() != SessionStatus.ACCEPTED) {
                throw new InvalidStateTransitionException("Only accepted sessions can be confirmed (current: " + sr.getStatus() + ")");
            }

            if (LocalDateTime.now().isBefore(sr.getProposedStart())) {
                throw new ValidationException(Map.of("confirm", "Cannot confirm session before the scheduled start time"));
            }

            boolean isLearner = (userId == sr.getLearnerId());
            sessionRequestDao.setConfirmed(requestId, isLearner);
            if (isLearner) {
                sr.setLearnerConfirmed(true);
            } else {
                sr.setProviderConfirmed(true);
            }

            if (sr.getFirstConfirmedAt() == null) {
                sessionRequestDao.markFirstConfirmed(requestId);
                sr.setFirstConfirmedAt(LocalDateTime.now());
            }

            if (sr.isLearnerConfirmed() && sr.isProviderConfirmed()) {
                transition(requestId, SessionStatus.ACCEPTED, SessionStatus.COMPLETED);
                sessionRequestDao.markCompleted(requestId);

                creditService.pay(sr.getProviderId(), requestId, sr.getCostCredits(), "Completed session #" + requestId);
                userDao.incrementTaught(sr.getProviderId());
                userDao.incrementLearned(sr.getLearnerId());

                badgeService.evaluate(sr.getProviderId());
                badgeService.evaluate(sr.getLearnerId());

                sr.setStatus(SessionStatus.COMPLETED);
                sr.setCompletedAt(LocalDateTime.now());

                if (notificationService != null) {
                    notificationService.notify(sr.getLearnerId(), NotificationType.SESSION_COMPLETED,
                            "Session Completed", "Your session for " + sr.getSkillName() + " is completed. Leave a review!",
                            "/requests/view?id=" + requestId);
                    notificationService.notify(sr.getProviderId(), NotificationType.SESSION_COMPLETED,
                            "Session Completed", "Your session for " + sr.getSkillName() + " is completed. " + sr.getCostCredits() + " credits earned!",
                            "/requests/view?id=" + requestId);
                }
            }

            return sr;
        });
    }

    /**
     * Reports a participant as no-show (eligible 15+ minutes after start).
     *
     * @param reporterId reporting user ID (learner or provider)
     * @param requestId session request ID
     * @return updated SessionRequest
     * @throws SkillSwapException if before 15 minutes, unauthorized, or invalid state
     */
    public SessionRequest markNoShow(long reporterId, long requestId) throws SkillSwapException {
        return TransactionManager.execute(() -> {
            SessionRequest sr = sessionRequestDao.lockById(requestId)
                    .orElseThrow(() -> new ResourceNotFoundException("Session request not found: " + requestId));

            if (reporterId != sr.getLearnerId() && reporterId != sr.getProviderId()) {
                throw new AuthorizationException("You are not a participant of this session");
            }

            if (sr.getStatus() != SessionStatus.ACCEPTED) {
                throw new InvalidStateTransitionException("Cannot report no-show for session in status " + sr.getStatus());
            }

            LocalDateTime minTime = sr.getProposedStart().plusMinutes(15);
            if (LocalDateTime.now().isBefore(minTime)) {
                throw new ValidationException(Map.of("noShow", "No-show can only be reported at least 15 minutes after start time"));
            }

            if (reporterId == sr.getProviderId()) {
                // Provider reports learner no-show: provider is paid
                transition(requestId, SessionStatus.ACCEPTED, SessionStatus.NO_SHOW_LEARNER);
                creditService.pay(sr.getProviderId(), requestId, sr.getCostCredits(), "Learner no-show compensation on session #" + requestId);
                sr.setStatus(SessionStatus.NO_SHOW_LEARNER);

                if (notificationService != null) {
                    notificationService.notify(sr.getLearnerId(), NotificationType.NO_SHOW,
                            "No-Show Reported", "You were reported as no-show for session #" + requestId,
                            "/requests/view?id=" + requestId);
                    notificationService.notify(sr.getProviderId(), NotificationType.NO_SHOW,
                            "No-Show Compensation", "Learner no-show confirmed. You have been paid " + sr.getCostCredits() + " credits.",
                            "/requests/view?id=" + requestId);
                }
            } else {
                // Learner reports provider no-show: learner refunded, provider gets strike
                transition(requestId, SessionStatus.ACCEPTED, SessionStatus.NO_SHOW_PROVIDER);
                creditService.release(sr.getLearnerId(), requestId, sr.getCostCredits(), "Provider no-show refund on session #" + requestId);
                userDao.incrementStrikes(sr.getProviderId());
                sr.setStatus(SessionStatus.NO_SHOW_PROVIDER);

                if (notificationService != null) {
                    notificationService.notify(sr.getProviderId(), NotificationType.NO_SHOW,
                            "No-Show Strike Recorded", "You were reported as no-show for session #" + requestId + ". A strike was added.",
                            "/requests/view?id=" + requestId);
                    notificationService.notify(sr.getLearnerId(), NotificationType.NO_SHOW,
                            "No-Show Refund", "Provider no-show confirmed. Your credits were refunded in full.",
                            "/requests/view?id=" + requestId);
                }
            }

            return sr;
        });
    }

    /**
     * Automatically expires stale PENDING requests older than configured expiry hours and refunds holds.
     *
     * @return number of expired requests
     */
    public int expireStale() {
        int expiryHours = Config.getInt("request.expiry.hours", 48);
        LocalDateTime cutoff = LocalDateTime.now().minusHours(expiryHours);
        List<SessionRequest> stale = sessionRequestDao.findPendingOlderThan(cutoff);
        int count = 0;

        for (SessionRequest sr : stale) {
            try {
                TransactionManager.execute(() -> {
                    transition(sr.getId(), SessionStatus.PENDING, SessionStatus.EXPIRED);
                    creditService.release(sr.getLearnerId(), sr.getId(), sr.getCostCredits(), "Request expired automatically");
                    if (notificationService != null) {
                        notificationService.notify(sr.getLearnerId(), NotificationType.REQUEST_EXPIRED,
                                "Request Expired", "Your pending request for " + sr.getSkillName() + " expired without a response.",
                                "/requests/view?id=" + sr.getId());
                    }
                    return null;
                });
                count++;
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to expire stale request #" + sr.getId(), e);
            }
        }
        return count;
    }

    /**
     * Automatically completes sessions where one party confirmed and 48 hours elapsed.
     *
     * @return number of auto-completed sessions
     */
    public int autoCompleteHalfConfirmed() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(48);
        List<SessionRequest> halfConfirmed = sessionRequestDao.findHalfConfirmedBefore(cutoff);
        int count = 0;

        for (SessionRequest sr : halfConfirmed) {
            try {
                TransactionManager.execute(() -> {
                    transition(sr.getId(), SessionStatus.ACCEPTED, SessionStatus.COMPLETED);
                    sessionRequestDao.markCompleted(sr.getId());

                    creditService.pay(sr.getProviderId(), sr.getId(), sr.getCostCredits(), "Auto-completed after 48h single confirmation");
                    userDao.incrementTaught(sr.getProviderId());
                    userDao.incrementLearned(sr.getLearnerId());

                    badgeService.evaluate(sr.getProviderId());
                    badgeService.evaluate(sr.getLearnerId());

                    if (notificationService != null) {
                        notificationService.notify(sr.getLearnerId(), NotificationType.SESSION_COMPLETED,
                                "Session Auto-Completed", "Your session for " + sr.getSkillName() + " was auto-completed.",
                                "/requests/view?id=" + sr.getId());
                        notificationService.notify(sr.getProviderId(), NotificationType.SESSION_COMPLETED,
                                "Session Auto-Completed", "Your session for " + sr.getSkillName() + " was auto-completed.",
                                "/requests/view?id=" + sr.getId());
                    }
                    return null;
                });
                count++;
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to auto-complete request #" + sr.getId(), e);
            }
        }
        return count;
    }

    /**
     * Retrieves session request by ID.
     *
     * @param id request ID
     * @return Optional containing SessionRequest
     */
    public Optional<SessionRequest> getById(long id) {
        return sessionRequestDao.findById(id);
    }

    /**
     * Retrieves incoming session requests for a provider with optional status filter.
     *
     * @param providerId provider user ID
     * @param statusOrNull optional status filter
     * @param limit maximum results
     * @param offset row offset
     * @return list of session requests
     */
    public List<SessionRequest> getIncoming(long providerId, SessionStatus statusOrNull, int limit, int offset) {
        return sessionRequestDao.findIncoming(providerId, statusOrNull, limit, offset);
    }

    /**
     * Counts incoming session requests for a provider.
     *
     * @param providerId provider user ID
     * @param statusOrNull optional status filter
     * @return count of requests
     */
    public int countIncoming(long providerId, SessionStatus statusOrNull) {
        return sessionRequestDao.countIncoming(providerId, statusOrNull);
    }

    /**
     * Retrieves outgoing session requests submitted by a learner.
     *
     * @param learnerId learner user ID
     * @param statusOrNull optional status filter
     * @param limit maximum results
     * @param offset row offset
     * @return list of session requests
     */
    public List<SessionRequest> getOutgoing(long learnerId, SessionStatus statusOrNull, int limit, int offset) {
        return sessionRequestDao.findOutgoing(learnerId, statusOrNull, limit, offset);
    }

    /**
     * Counts outgoing session requests for a learner.
     *
     * @param learnerId learner user ID
     * @param statusOrNull optional status filter
     * @return count of requests
     */
    public int countOutgoing(long learnerId, SessionStatus statusOrNull) {
        return sessionRequestDao.countOutgoing(learnerId, statusOrNull);
    }

    /**
     * Retrieves upcoming accepted sessions for a user.
     *
     * @param userId user ID
     * @param limit maximum results
     * @return list of session requests
     */
    public List<SessionRequest> getUpcomingAccepted(long userId, int limit) {
        return sessionRequestDao.findUpcomingAccepted(userId, limit);
    }

    /**
     * Counts pending incoming requests for quick badge notifications.
     *
     * @param providerId provider user ID
     * @return count of pending incoming requests
     */
    public int countPendingIncoming(long providerId) {
        return sessionRequestDao.countPendingIncoming(providerId);
    }
}
