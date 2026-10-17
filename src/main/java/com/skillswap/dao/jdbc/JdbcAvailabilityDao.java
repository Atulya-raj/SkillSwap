package com.skillswap.dao.jdbc;

import com.skillswap.dao.AvailabilityDao;
import com.skillswap.db.Database;
import com.skillswap.exception.DataAccessException;
import com.skillswap.model.AvailabilitySlot;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * JDBC implementation of AvailabilityDao for managing user availability slots.
 */
public class JdbcAvailabilityDao implements AvailabilityDao {

    @Override
    public List<AvailabilitySlot> findByUser(long userId) {
        String sql = "SELECT id, user_id, day_of_week, start_time, end_time FROM availability_slots "
                + "WHERE user_id = ? ORDER BY day_of_week, start_time";
        List<AvailabilitySlot> list = new ArrayList<>();
        try (Connection con = Database.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to find availability slots for user " + userId, e);
        }
    }

    @Override
    public long insert(AvailabilitySlot slot) {
        String sql = "INSERT INTO availability_slots (user_id, day_of_week, start_time, end_time) VALUES (?, ?, ?, ?)";
        try (Connection con = Database.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, slot.getUserId());
            ps.setInt(2, slot.getDayOfWeek().getValue());
            ps.setTime(3, Time.valueOf(slot.getStartTime()));
            ps.setTime(4, Time.valueOf(slot.getEndTime()));
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    slot.setId(rs.getLong(1));
                    return slot.getId();
                }
            }
            throw new DataAccessException("Failed to retrieve generated id for availability slot");
        } catch (SQLException e) {
            throw new DataAccessException("Failed to insert availability slot", e);
        }
    }

    @Override
    public void delete(long id, long userId) {
        String sql = "DELETE FROM availability_slots WHERE id = ? AND user_id = ?";
        try (Connection con = Database.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.setLong(2, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Failed to delete availability slot " + id, e);
        }
    }

    @Override
    public boolean hasOverlap(long userId, DayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime, Long excludeId) {
        StringBuilder sql = new StringBuilder(
                "SELECT COUNT(*) FROM availability_slots WHERE user_id = ? AND day_of_week = ? "
                        + "AND start_time < ? AND end_time > ?"
        );
        if (excludeId != null) {
            sql.append(" AND id <> ?");
        }
        try (Connection con = Database.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql.toString())) {
            ps.setLong(1, userId);
            ps.setInt(2, dayOfWeek.getValue());
            ps.setTime(3, Time.valueOf(endTime));
            ps.setTime(4, Time.valueOf(startTime));
            if (excludeId != null) {
                ps.setLong(5, excludeId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
            return false;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to check availability overlap", e);
        }
    }

    private AvailabilitySlot mapRow(ResultSet rs) throws SQLException {
        AvailabilitySlot slot = new AvailabilitySlot();
        slot.setId(rs.getLong("id"));
        slot.setUserId(rs.getLong("user_id"));
        slot.setDayOfWeek(DayOfWeek.of(rs.getInt("day_of_week")));
        slot.setStartTime(rs.getTime("start_time").toLocalTime());
        slot.setEndTime(rs.getTime("end_time").toLocalTime());
        return slot;
    }
}
