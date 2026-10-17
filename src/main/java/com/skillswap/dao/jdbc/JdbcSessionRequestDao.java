package com.skillswap.dao.jdbc;

import com.skillswap.dao.SessionRequestDao;
import com.skillswap.db.Database;
import com.skillswap.exception.DataAccessException;
import com.skillswap.model.SessionRequest;
import com.skillswap.model.enums.SessionMode;
import com.skillswap.model.enums.SessionStatus;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC implementation of SessionRequestDao with locking, state guards, and conflict detection.
 */
public class JdbcSessionRequestDao implements SessionRequestDao {

    private static final String BASE_SELECT =
            "SELECT sr.id, sr.learner_id, sr.provider_id, sr.skill_id, sr.listing_id, "
                    + "sr.proposed_start, sr.duration_minutes, sr.mode, sr.meeting_details, "
                    + "sr.message, sr.cost_credits, sr.status, sr.learner_confirmed, "
                    + "sr.provider_confirmed, sr.reminder_sent, sr.created_at, sr.responded_at, "
                    + "sr.first_confirmed_at, sr.completed_at, "
                    + "ul.full_name AS learner_name, ul.avatar_path AS learner_avatar, "
                    + "up.full_name AS provider_name, up.avatar_path AS provider_avatar, "
                    + "s.name AS skill_name "
                    + "FROM session_requests sr "
                    + "JOIN users ul ON sr.learner_id = ul.id "
                    + "JOIN users up ON sr.provider_id = up.id "
                    + "JOIN skills s ON sr.skill_id = s.id ";

    @Override
    public long insert(SessionRequest r) {
        String sql = "INSERT INTO session_requests (learner_id, provider_id, skill_id, listing_id, "
                + "proposed_start, duration_minutes, mode, meeting_details, message, cost_credits, "
                + "status, learner_confirmed, provider_confirmed, reminder_sent, created_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection con = Database.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, r.getLearnerId());
            ps.setLong(2, r.getProviderId());
            ps.setInt(3, r.getSkillId());
            ps.setLong(4, r.getListingId());
            ps.setTimestamp(5, Timestamp.valueOf(r.getProposedStart()));
            ps.setInt(6, r.getDurationMinutes());
            ps.setString(7, r.getMode().name());
            ps.setString(8, r.getMeetingDetails());
            ps.setString(9, r.getMessage());
            ps.setInt(10, r.getCostCredits());
            ps.setString(11, r.getStatus().name());
            ps.setBoolean(12, r.isLearnerConfirmed());
            ps.setBoolean(13, r.isProviderConfirmed());
            ps.setBoolean(14, r.isReminderSent());
            LocalDateTime created = r.getCreatedAt() != null ? r.getCreatedAt() : LocalDateTime.now();
            ps.setTimestamp(15, Timestamp.valueOf(created));

            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    r.setId(rs.getLong(1));
                    return r.getId();
                }
            }
            throw new DataAccessException("Failed to retrieve generated id for session request");
        } catch (SQLException e) {
            throw new DataAccessException("Failed to insert session request", e);
        }
    }

    @Override
    public Optional<SessionRequest> findById(long id) {
        String sql = BASE_SELECT + "WHERE sr.id = ?";
        try (Connection con = Database.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new DataAccessException("Failed to find session request by id " + id, e);
        }
    }

    @Override
    public Optional<SessionRequest> lockById(long id) {
        String sql = BASE_SELECT + "WHERE sr.id = ? FOR UPDATE";
        try (Connection con = Database.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new DataAccessException("Failed to lock session request by id " + id, e);
        }
    }

    @Override
    public boolean updateStatus(long id, SessionStatus from, SessionStatus to) {
        String sql = "UPDATE session_requests SET status = ? WHERE id = ? AND status = ?";
        try (Connection con = Database.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, to.name());
            ps.setLong(2, id);
            ps.setString(3, from.name());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to update session request status", e);
        }
    }

    @Override
    public void setConfirmed(long id, boolean learnerSide) {
        String column = learnerSide ? "learner_confirmed" : "provider_confirmed";
        String sql = "UPDATE session_requests SET " + column + " = TRUE WHERE id = ?";
        try (Connection con = Database.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Failed to set confirmation on session " + id, e);
        }
    }

    @Override
    public void markFirstConfirmed(long id) {
        String sql = "UPDATE session_requests SET first_confirmed_at = NOW() WHERE id = ? AND first_confirmed_at IS NULL";
        try (Connection con = Database.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Failed to mark first confirmation on session " + id, e);
        }
    }

    @Override
    public void markCompleted(long id) {
        String sql = "UPDATE session_requests SET status = 'COMPLETED', completed_at = NOW() WHERE id = ?";
        try (Connection con = Database.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Failed to mark session completed: " + id, e);
        }
    }

    @Override
    public void markResponded(long id) {
        String sql = "UPDATE session_requests SET responded_at = NOW() WHERE id = ? AND responded_at IS NULL";
        try (Connection con = Database.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Failed to mark responded on session " + id, e);
        }
    }

    @Override
    public void markReminderSent(long id) {
        String sql = "UPDATE session_requests SET reminder_sent = TRUE WHERE id = ?";
        try (Connection con = Database.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Failed to mark reminder sent on session " + id, e);
        }
    }

    @Override
    public List<SessionRequest> findIncoming(long providerId, SessionStatus statusOrNull, int limit, int offset) {
        StringBuilder sql = new StringBuilder(BASE_SELECT).append("WHERE sr.provider_id = ? ");
        if (statusOrNull != null) {
            sql.append("AND sr.status = ? ");
        }
        sql.append("ORDER BY sr.created_at DESC LIMIT ? OFFSET ?");

        List<SessionRequest> list = new ArrayList<>();
        try (Connection con = Database.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql.toString())) {
            int p = 1;
            ps.setLong(p++, providerId);
            if (statusOrNull != null) {
                ps.setString(p++, statusOrNull.name());
            }
            ps.setInt(p++, limit);
            ps.setInt(p, offset);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to find incoming sessions", e);
        }
    }

    @Override
    public List<SessionRequest> findOutgoing(long learnerId, SessionStatus statusOrNull, int limit, int offset) {
        StringBuilder sql = new StringBuilder(BASE_SELECT).append("WHERE sr.learner_id = ? ");
        if (statusOrNull != null) {
            sql.append("AND sr.status = ? ");
        }
        sql.append("ORDER BY sr.created_at DESC LIMIT ? OFFSET ?");

        List<SessionRequest> list = new ArrayList<>();
        try (Connection con = Database.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql.toString())) {
            int p = 1;
            ps.setLong(p++, learnerId);
            if (statusOrNull != null) {
                ps.setString(p++, statusOrNull.name());
            }
            ps.setInt(p++, limit);
            ps.setInt(p, offset);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to find outgoing sessions", e);
        }
    }

    @Override
    public List<SessionRequest> findUpcomingAccepted(long userId, int limit) {
        String sql = BASE_SELECT
                + "WHERE (sr.learner_id = ? OR sr.provider_id = ?) AND sr.status = 'ACCEPTED' "
                + "AND sr.proposed_start >= NOW() "
                + "ORDER BY sr.proposed_start ASC LIMIT ?";
        List<SessionRequest> list = new ArrayList<>();
        try (Connection con = Database.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setLong(2, userId);
            ps.setInt(3, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to find upcoming accepted sessions", e);
        }
    }

    @Override
    public boolean existsActive(long learnerId, long providerId, int skillId) {
        String sql = "SELECT COUNT(*) FROM session_requests "
                + "WHERE learner_id = ? AND provider_id = ? AND skill_id = ? "
                + "AND status IN ('PENDING', 'ACCEPTED')";
        try (Connection con = Database.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, learnerId);
            ps.setLong(2, providerId);
            ps.setInt(3, skillId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
            return false;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to check active session existence", e);
        }
    }

    @Override
    public boolean hasOverlappingAccepted(long providerId, LocalDateTime start, int minutes, Long excludeRequestId) {
        StringBuilder sql = new StringBuilder(
                "SELECT COUNT(*) FROM session_requests "
                        + "WHERE provider_id = ? AND status = 'ACCEPTED' "
                        + "AND proposed_start < DATE_ADD(?, INTERVAL ? MINUTE) "
                        + "AND DATE_ADD(proposed_start, INTERVAL duration_minutes MINUTE) > ?"
        );
        if (excludeRequestId != null) {
            sql.append(" AND id <> ?");
        }

        try (Connection con = Database.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql.toString())) {
            ps.setLong(1, providerId);
            ps.setTimestamp(2, Timestamp.valueOf(start));
            ps.setInt(3, minutes);
            ps.setTimestamp(4, Timestamp.valueOf(start));
            if (excludeRequestId != null) {
                ps.setLong(5, excludeRequestId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
            return false;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to check overlapping accepted sessions", e);
        }
    }

    @Override
    public List<SessionRequest> findPendingOlderThan(LocalDateTime cutoff) {
        String sql = BASE_SELECT + "WHERE sr.status = 'PENDING' AND sr.created_at < ? ORDER BY sr.created_at ASC";
        List<SessionRequest> list = new ArrayList<>();
        try (Connection con = Database.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(cutoff));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to find pending sessions older than cutoff", e);
        }
    }

    @Override
    public List<SessionRequest> findAcceptedStartingBetween(LocalDateTime from, LocalDateTime to) {
        String sql = BASE_SELECT + "WHERE sr.status = 'ACCEPTED' AND sr.proposed_start BETWEEN ? AND ? "
                + "ORDER BY sr.proposed_start ASC";
        List<SessionRequest> list = new ArrayList<>();
        try (Connection con = Database.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(from));
            ps.setTimestamp(2, Timestamp.valueOf(to));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to find accepted sessions starting between window", e);
        }
    }

    @Override
    public List<SessionRequest> findHalfConfirmedBefore(LocalDateTime cutoff) {
        String sql = BASE_SELECT + "WHERE sr.status = 'ACCEPTED' "
                + "AND (sr.learner_confirmed <> sr.provider_confirmed) "
                + "AND sr.first_confirmed_at IS NOT NULL AND sr.first_confirmed_at < ? "
                + "ORDER BY sr.first_confirmed_at ASC";
        List<SessionRequest> list = new ArrayList<>();
        try (Connection con = Database.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(cutoff));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to find half confirmed sessions before cutoff", e);
        }
    }

    @Override
    public List<SessionRequest> findAllForUser(long userId) {
        String sql = BASE_SELECT + "WHERE (sr.learner_id = ? OR sr.provider_id = ?) "
                + "ORDER BY sr.created_at DESC";
        List<SessionRequest> list = new ArrayList<>();
        try (Connection con = Database.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setLong(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to find all sessions for user " + userId, e);
        }
    }

    @Override
    public int countPendingIncoming(long providerId) {
        return countIncoming(providerId, SessionStatus.PENDING);
    }

    @Override
    public int countIncoming(long providerId, SessionStatus statusOrNull) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM session_requests WHERE provider_id = ?");
        if (statusOrNull != null) {
            sql.append(" AND status = ?");
        }
        try (Connection con = Database.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql.toString())) {
            ps.setLong(1, providerId);
            if (statusOrNull != null) {
                ps.setString(2, statusOrNull.name());
            }
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
            return 0;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to count incoming sessions", e);
        }
    }

    @Override
    public int countOutgoing(long learnerId, SessionStatus statusOrNull) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM session_requests WHERE learner_id = ?");
        if (statusOrNull != null) {
            sql.append(" AND status = ?");
        }
        try (Connection con = Database.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql.toString())) {
            ps.setLong(1, learnerId);
            if (statusOrNull != null) {
                ps.setString(2, statusOrNull.name());
            }
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
            return 0;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to count outgoing sessions", e);
        }
    }

    private SessionRequest mapRow(ResultSet rs) throws SQLException {
        SessionRequest sr = new SessionRequest();
        sr.setId(rs.getLong("id"));
        sr.setLearnerId(rs.getLong("learner_id"));
        sr.setProviderId(rs.getLong("provider_id"));
        sr.setSkillId(rs.getInt("skill_id"));
        sr.setListingId(rs.getLong("listing_id"));
        Timestamp startTs = rs.getTimestamp("proposed_start");
        if (startTs != null) {
            sr.setProposedStart(startTs.toLocalDateTime());
        }
        sr.setDurationMinutes(rs.getInt("duration_minutes"));
        sr.setMode(SessionMode.valueOf(rs.getString("mode")));
        sr.setMeetingDetails(rs.getString("meeting_details"));
        sr.setMessage(rs.getString("message"));
        sr.setCostCredits(rs.getInt("cost_credits"));
        sr.setStatus(SessionStatus.valueOf(rs.getString("status")));
        sr.setLearnerConfirmed(rs.getBoolean("learner_confirmed"));
        sr.setProviderConfirmed(rs.getBoolean("provider_confirmed"));
        sr.setReminderSent(rs.getBoolean("reminder_sent"));

        Timestamp createdTs = rs.getTimestamp("created_at");
        if (createdTs != null) {
            sr.setCreatedAt(createdTs.toLocalDateTime());
        }
        Timestamp respTs = rs.getTimestamp("responded_at");
        if (respTs != null) {
            sr.setRespondedAt(respTs.toLocalDateTime());
        }
        Timestamp firstConfTs = rs.getTimestamp("first_confirmed_at");
        if (firstConfTs != null) {
            sr.setFirstConfirmedAt(firstConfTs.toLocalDateTime());
        }
        Timestamp compTs = rs.getTimestamp("completed_at");
        if (compTs != null) {
            sr.setCompletedAt(compTs.toLocalDateTime());
        }

        // Display fields
        sr.setLearnerName(rs.getString("learner_name"));
        sr.setLearnerAvatar(rs.getString("learner_avatar"));
        sr.setProviderName(rs.getString("provider_name"));
        sr.setProviderAvatar(rs.getString("provider_avatar"));
        sr.setSkillName(rs.getString("skill_name"));

        return sr;
    }
}
