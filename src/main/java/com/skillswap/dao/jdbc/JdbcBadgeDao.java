package com.skillswap.dao.jdbc;

import com.skillswap.dao.BadgeDao;
import com.skillswap.db.Database;
import com.skillswap.exception.DataAccessException;
import com.skillswap.model.Badge;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * JDBC implementation of BadgeDao for achievement awards and lookups.
 */
public class JdbcBadgeDao implements BadgeDao {

    @Override
    public List<Badge> findAll() {
        String sql = "SELECT id, code, name, description FROM badges ORDER BY id ASC";
        List<Badge> list = new ArrayList<>();
        try (Connection con = Database.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Badge b = new Badge(
                        rs.getInt("id"),
                        rs.getString("code"),
                        rs.getString("name"),
                        rs.getString("description")
                );
                list.add(b);
            }
            return list;
        } catch (SQLException ex) {
            throw new DataAccessException("Failed to find all badges", ex);
        }
    }

    @Override
    public List<Badge> findForUser(long userId) {
        String sql = "SELECT b.id, b.code, b.name, b.description, ub.awarded_at "
                + "FROM badges b "
                + "JOIN user_badges ub ON b.id = ub.badge_id "
                + "WHERE ub.user_id = ? "
                + "ORDER BY ub.awarded_at DESC";
        List<Badge> list = new ArrayList<>();
        try (Connection con = Database.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Badge b = new Badge(
                            rs.getInt("id"),
                            rs.getString("code"),
                            rs.getString("name"),
                            rs.getString("description")
                    );
                    Timestamp ts = rs.getTimestamp("awarded_at");
                    if (ts != null) {
                        b.setAwardedAt(ts.toLocalDateTime());
                    }
                    list.add(b);
                }
            }
            return list;
        } catch (SQLException ex) {
            throw new DataAccessException("Failed to find badges for user " + userId, ex);
        }
    }

    @Override
    public boolean has(long userId, String code) {
        String sql = "SELECT COUNT(*) FROM user_badges ub "
                + "JOIN badges b ON ub.badge_id = b.id "
                + "WHERE ub.user_id = ? AND b.code = ?";
        try (Connection con = Database.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setString(2, code);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
            return false;
        } catch (SQLException ex) {
            throw new DataAccessException("Failed to check badge " + code + " for user " + userId, ex);
        }
    }

    @Override
    public void award(long userId, String code) {
        String sql = "INSERT IGNORE INTO user_badges (user_id, badge_id, awarded_at) "
                + "SELECT ?, id, NOW() FROM badges WHERE code = ?";
        try (Connection con = Database.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setString(2, code);
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new DataAccessException("Failed to award badge " + code + " to user " + userId, ex);
        }
    }
}
