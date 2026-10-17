package com.skillswap.dao.jdbc;

import com.skillswap.dao.MessageDao;
import com.skillswap.db.Database;
import com.skillswap.exception.DataAccessException;
import com.skillswap.model.Message;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * JDBC implementation of MessageDao for in-app thread messaging.
 */
public class JdbcMessageDao implements MessageDao {

    @Override
    public long insert(Message m) {
        String sql = "INSERT INTO messages (session_request_id, sender_id, body, is_read, created_at) "
                + "VALUES (?, ?, ?, ?, ?)";
        try (Connection con = Database.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, m.getSessionRequestId());
            ps.setLong(2, m.getSenderId());
            ps.setString(3, m.getBody());
            ps.setBoolean(4, m.isRead());
            LocalDateTime created = m.getCreatedAt() != null ? m.getCreatedAt() : LocalDateTime.now();
            ps.setTimestamp(5, Timestamp.valueOf(created));

            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    m.setId(rs.getLong(1));
                    return m.getId();
                }
            }
            throw new DataAccessException("Failed to retrieve generated id for message");
        } catch (SQLException ex) {
            throw new DataAccessException("Failed to insert message", ex);
        }
    }

    @Override
    public List<Message> findThread(long sessionId) {
        String sql = "SELECT m.id, m.session_request_id, m.sender_id, m.body, m.is_read, m.created_at, "
                + "u.full_name AS sender_name "
                + "FROM messages m "
                + "JOIN users u ON m.sender_id = u.id "
                + "WHERE m.session_request_id = ? "
                + "ORDER BY m.created_at ASC, m.id ASC";
        List<Message> list = new ArrayList<>();
        try (Connection con = Database.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, sessionId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
            return list;
        } catch (SQLException ex) {
            throw new DataAccessException("Failed to find messages for session thread " + sessionId, ex);
        }
    }

    @Override
    public int countUnread(long userId) {
        String sql = "SELECT COUNT(*) FROM messages m "
                + "JOIN session_requests sr ON m.session_request_id = sr.id "
                + "WHERE (sr.learner_id = ? OR sr.provider_id = ?) "
                + "AND m.sender_id <> ? "
                + "AND m.is_read = FALSE";
        try (Connection con = Database.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setLong(2, userId);
            ps.setLong(3, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
            return 0;
        } catch (SQLException ex) {
            throw new DataAccessException("Failed to count unread messages for user " + userId, ex);
        }
    }

    @Override
    public void markThreadRead(long sessionId, long readerId) {
        String sql = "UPDATE messages SET is_read = TRUE "
                + "WHERE session_request_id = ? AND sender_id <> ? AND is_read = FALSE";
        try (Connection con = Database.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, sessionId);
            ps.setLong(2, readerId);
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new DataAccessException("Failed to mark messages read in session " + sessionId, ex);
        }
    }

    private Message mapRow(ResultSet rs) throws SQLException {
        Message m = new Message();
        m.setId(rs.getLong("id"));
        m.setSessionRequestId(rs.getLong("session_request_id"));
        m.setSenderId(rs.getLong("sender_id"));
        m.setBody(rs.getString("body"));
        m.setRead(rs.getBoolean("is_read"));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) {
            m.setCreatedAt(ts.toLocalDateTime());
        }
        m.setSenderName(rs.getString("sender_name"));
        return m;
    }
}
