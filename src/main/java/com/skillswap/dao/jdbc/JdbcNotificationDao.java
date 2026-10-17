package com.skillswap.dao.jdbc;

import com.skillswap.dao.NotificationDao;
import com.skillswap.db.Database;
import com.skillswap.exception.DataAccessException;
import com.skillswap.model.Notification;
import com.skillswap.model.enums.NotificationType;

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
 * JDBC implementation of NotificationDao for in-app notifications.
 */
public class JdbcNotificationDao implements NotificationDao {

    @Override
    public long insert(Notification n) {
        String sql = "INSERT INTO notifications (user_id, type, title, body, link_url, is_read, created_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection con = Database.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, n.getUserId());
            ps.setString(2, n.getType().name());
            ps.setString(3, n.getTitle());
            ps.setString(4, n.getBody());
            ps.setString(5, n.getLinkUrl());
            ps.setBoolean(6, n.isRead());
            LocalDateTime created = n.getCreatedAt() != null ? n.getCreatedAt() : LocalDateTime.now();
            ps.setTimestamp(7, Timestamp.valueOf(created));

            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    n.setId(rs.getLong(1));
                    return n.getId();
                }
            }
            throw new DataAccessException("Failed to retrieve generated id for notification");
        } catch (SQLException ex) {
            throw new DataAccessException("Failed to insert notification", ex);
        }
    }

    @Override
    public List<Notification> findByUser(long userId, int limit, int offset) {
        String sql = "SELECT id, user_id, type, title, body, link_url, is_read, created_at "
                + "FROM notifications WHERE user_id = ? ORDER BY created_at DESC LIMIT ? OFFSET ?";
        List<Notification> list = new ArrayList<>();
        try (Connection con = Database.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setInt(2, limit);
            ps.setInt(3, offset);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
            return list;
        } catch (SQLException ex) {
            throw new DataAccessException("Failed to find notifications for user " + userId, ex);
        }
    }

    @Override
    public int countByUser(long userId) {
        String sql = "SELECT COUNT(*) FROM notifications WHERE user_id = ?";
        try (Connection con = Database.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
            return 0;
        } catch (SQLException ex) {
            throw new DataAccessException("Failed to count notifications for user " + userId, ex);
        }
    }

    @Override
    public int countUnread(long userId) {
        String sql = "SELECT COUNT(*) FROM notifications WHERE user_id = ? AND is_read = FALSE";
        try (Connection con = Database.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
            return 0;
        } catch (SQLException ex) {
            throw new DataAccessException("Failed to count unread notifications for user " + userId, ex);
        }
    }

    @Override
    public void markAllRead(long userId) {
        String sql = "UPDATE notifications SET is_read = TRUE WHERE user_id = ?";
        try (Connection con = Database.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new DataAccessException("Failed to mark all notifications read for user " + userId, ex);
        }
    }

    @Override
    public void markRead(long id, long userId) {
        String sql = "UPDATE notifications SET is_read = TRUE WHERE id = ? AND user_id = ?";
        try (Connection con = Database.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.setLong(2, userId);
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new DataAccessException("Failed to mark notification " + id + " read", ex);
        }
    }

    private Notification mapRow(ResultSet rs) throws SQLException {
        Notification n = new Notification();
        n.setId(rs.getLong("id"));
        n.setUserId(rs.getLong("user_id"));
        n.setType(NotificationType.valueOf(rs.getString("type")));
        n.setTitle(rs.getString("title"));
        n.setBody(rs.getString("body"));
        n.setLinkUrl(rs.getString("link_url"));
        n.setRead(rs.getBoolean("is_read"));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) {
            n.setCreatedAt(ts.toLocalDateTime());
        }
        return n;
    }
}
