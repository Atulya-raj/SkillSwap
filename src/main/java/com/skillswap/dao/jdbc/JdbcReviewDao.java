package com.skillswap.dao.jdbc;

import com.skillswap.dao.ReviewDao;
import com.skillswap.db.Database;
import com.skillswap.exception.DataAccessException;
import com.skillswap.model.Review;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
 * JDBC implementation of ReviewDao for managing ratings and peer reviews.
 */
public class JdbcReviewDao implements ReviewDao {

    @Override
    public long insert(Review r) {
        String sql = "INSERT INTO reviews (session_request_id, reviewer_id, reviewee_id, rating, comment, is_hidden, created_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection con = Database.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, r.getSessionRequestId());
            ps.setLong(2, r.getReviewerId());
            ps.setLong(3, r.getRevieweeId());
            ps.setInt(4, r.getRating());
            ps.setString(5, r.getComment());
            ps.setBoolean(6, r.isHidden());
            LocalDateTime created = r.getCreatedAt() != null ? r.getCreatedAt() : LocalDateTime.now();
            ps.setTimestamp(7, Timestamp.valueOf(created));

            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    r.setId(rs.getLong(1));
                    return r.getId();
                }
            }
            throw new DataAccessException("Failed to retrieve generated id for review");
        } catch (SQLException ex) {
            throw new DataAccessException("Failed to insert review", ex);
        }
    }

    @Override
    public boolean exists(long sessionId, long reviewerId) {
        String sql = "SELECT COUNT(*) FROM reviews WHERE session_request_id = ? AND reviewer_id = ?";
        try (Connection con = Database.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, sessionId);
            ps.setLong(2, reviewerId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
            return false;
        } catch (SQLException ex) {
            throw new DataAccessException("Failed to check review existence", ex);
        }
    }

    @Override
    public List<Review> findForUser(long userId, int limit) {
        String sql = "SELECT r.id, r.session_request_id, r.reviewer_id, r.reviewee_id, r.rating, r.comment, "
                + "r.is_hidden, r.created_at, u.full_name AS reviewer_name "
                + "FROM reviews r "
                + "JOIN users u ON r.reviewer_id = u.id "
                + "WHERE r.reviewee_id = ? AND r.is_hidden = FALSE "
                + "ORDER BY r.created_at DESC LIMIT ?";
        List<Review> list = new ArrayList<>();
        try (Connection con = Database.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
            return list;
        } catch (SQLException ex) {
            throw new DataAccessException("Failed to find reviews for user " + userId, ex);
        }
    }

    @Override
    public BigDecimal avgFor(long userId) {
        String sql = "SELECT COALESCE(AVG(rating), 0.0) FROM reviews WHERE reviewee_id = ? AND is_hidden = FALSE";
        try (Connection con = Database.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    double avg = rs.getDouble(1);
                    return BigDecimal.valueOf(avg).setScale(2, RoundingMode.HALF_UP);
                }
            }
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        } catch (SQLException ex) {
            throw new DataAccessException("Failed to calculate avg rating for user " + userId, ex);
        }
    }

    @Override
    public int countFor(long userId) {
        String sql = "SELECT COUNT(*) FROM reviews WHERE reviewee_id = ? AND is_hidden = FALSE";
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
            throw new DataAccessException("Failed to count reviews for user " + userId, ex);
        }
    }

    @Override
    public void setHidden(long id, boolean hidden) {
        String sql = "UPDATE reviews SET is_hidden = ? WHERE id = ?";
        try (Connection con = Database.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setBoolean(1, hidden);
            ps.setLong(2, id);
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new DataAccessException("Failed to set hidden status on review " + id, ex);
        }
    }

    @Override
    public Optional<Review> findById(long id) {
        String sql = "SELECT r.id, r.session_request_id, r.reviewer_id, r.reviewee_id, r.rating, r.comment, "
                + "r.is_hidden, r.created_at, u.full_name AS reviewer_name "
                + "FROM reviews r "
                + "JOIN users u ON r.reviewer_id = u.id "
                + "WHERE r.id = ?";
        try (Connection con = Database.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
            return Optional.empty();
        } catch (SQLException ex) {
            throw new DataAccessException("Failed to find review by id " + id, ex);
        }
    }

    @Override
    public int countFiveStar(long userId) {
        String sql = "SELECT COUNT(*) FROM reviews WHERE reviewee_id = ? AND rating = 5 AND is_hidden = FALSE";
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
            throw new DataAccessException("Failed to count five star reviews for user " + userId, ex);
        }
    }

    private Review mapRow(ResultSet rs) throws SQLException {
        Review r = new Review();
        r.setId(rs.getLong("id"));
        r.setSessionRequestId(rs.getLong("session_request_id"));
        r.setReviewerId(rs.getLong("reviewer_id"));
        r.setRevieweeId(rs.getLong("reviewee_id"));
        r.setRating(rs.getInt("rating"));
        r.setComment(rs.getString("comment"));
        r.setHidden(rs.getBoolean("is_hidden"));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) {
            r.setCreatedAt(ts.toLocalDateTime());
        }
        r.setReviewerName(rs.getString("reviewer_name"));
        return r;
    }
}
