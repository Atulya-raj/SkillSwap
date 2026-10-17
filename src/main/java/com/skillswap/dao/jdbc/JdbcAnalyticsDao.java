package com.skillswap.dao.jdbc;

import com.skillswap.dao.AnalyticsDao;
import com.skillswap.db.Database;
import com.skillswap.exception.DataAccessException;
import com.skillswap.model.enums.SessionStatus;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * JDBC implementation of AnalyticsDao for metrics and administrative dashboards.
 */
public class JdbcAnalyticsDao implements AnalyticsDao {

    @Override
    public int totalUsers() {
        String sql = "SELECT COUNT(*) FROM users WHERE role = 'STUDENT'";
        try (Connection con = Database.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
            return 0;
        } catch (SQLException ex) {
            throw new DataAccessException("Failed to count total users", ex);
        }
    }

    @Override
    public Map<SessionStatus, Integer> sessionsByStatus() {
        Map<SessionStatus, Integer> map = new EnumMap<>(SessionStatus.class);
        for (SessionStatus status : SessionStatus.values()) {
            map.put(status, 0);
        }

        String sql = "SELECT status, COUNT(*) FROM session_requests GROUP BY status";
        try (Connection con = Database.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                SessionStatus st = SessionStatus.valueOf(rs.getString(1));
                map.put(st, rs.getInt(2));
            }
            return map;
        } catch (SQLException ex) {
            throw new DataAccessException("Failed to count sessions by status", ex);
        }
    }

    @Override
    public List<SkillCount> topWanted(int n) {
        String sql = "SELECT s.name, COUNT(*) AS cnt "
                + "FROM user_skills us "
                + "JOIN skills s ON us.skill_id = s.id "
                + "WHERE us.listing_type = 'WANTED' "
                + "GROUP BY s.id, s.name "
                + "ORDER BY cnt DESC, s.name ASC "
                + "LIMIT ?";
        List<SkillCount> list = new ArrayList<>();
        try (Connection con = Database.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, n);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new SkillCount(rs.getString("name"), rs.getInt("cnt")));
                }
            }
            return list;
        } catch (SQLException ex) {
            throw new DataAccessException("Failed to query top wanted skills", ex);
        }
    }

    @Override
    public List<SkillCount> topOffered(int n) {
        String sql = "SELECT s.name, COUNT(*) AS cnt "
                + "FROM user_skills us "
                + "JOIN skills s ON us.skill_id = s.id "
                + "WHERE us.listing_type = 'OFFERED' "
                + "GROUP BY s.id, s.name "
                + "ORDER BY cnt DESC, s.name ASC "
                + "LIMIT ?";
        List<SkillCount> list = new ArrayList<>();
        try (Connection con = Database.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, n);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new SkillCount(rs.getString("name"), rs.getInt("cnt")));
                }
            }
            return list;
        } catch (SQLException ex) {
            throw new DataAccessException("Failed to query top offered skills", ex);
        }
    }

    @Override
    public List<WeekCount> newUsersPerWeek(int weeks) {
        String sql = "SELECT DATE_FORMAT(created_at, '%Y-W%u') AS wk, COUNT(*) AS cnt "
                + "FROM users "
                + "WHERE role = 'STUDENT' AND created_at >= DATE_SUB(NOW(), INTERVAL ? WEEK) "
                + "GROUP BY wk "
                + "ORDER BY wk ASC";
        List<WeekCount> list = new ArrayList<>();
        try (Connection con = Database.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, weeks);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new WeekCount(rs.getString("wk"), rs.getInt("cnt")));
                }
            }
            return list;
        } catch (SQLException ex) {
            throw new DataAccessException("Failed to query new users per week", ex);
        }
    }
}
