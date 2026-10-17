package com.skillswap.dao;

import com.skillswap.model.SessionRequest;
import com.skillswap.model.enums.SessionStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Data access interface for session requests and state changes.
 */
public interface SessionRequestDao {

    long insert(SessionRequest r);

    Optional<SessionRequest> findById(long id);

    Optional<SessionRequest> lockById(long id);

    boolean updateStatus(long id, SessionStatus from, SessionStatus to);

    void setConfirmed(long id, boolean learnerSide);

    void markFirstConfirmed(long id);

    void markCompleted(long id);

    void markResponded(long id);

    void markReminderSent(long id);

    List<SessionRequest> findIncoming(long providerId, SessionStatus statusOrNull, int limit, int offset);

    List<SessionRequest> findOutgoing(long learnerId, SessionStatus statusOrNull, int limit, int offset);

    List<SessionRequest> findUpcomingAccepted(long userId, int limit);

    boolean existsActive(long learnerId, long providerId, int skillId);

    boolean hasOverlappingAccepted(long providerId, LocalDateTime start, int minutes, Long excludeRequestId);

    List<SessionRequest> findPendingOlderThan(LocalDateTime cutoff);

    List<SessionRequest> findAcceptedStartingBetween(LocalDateTime from, LocalDateTime to);

    List<SessionRequest> findHalfConfirmedBefore(LocalDateTime cutoff);

    List<SessionRequest> findAllForUser(long userId);

    int countPendingIncoming(long providerId);

    int countIncoming(long providerId, SessionStatus statusOrNull);

    int countOutgoing(long learnerId, SessionStatus statusOrNull);
}
