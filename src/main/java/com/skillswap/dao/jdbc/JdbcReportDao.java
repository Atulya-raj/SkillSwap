package com.skillswap.dao.jdbc;

import com.skillswap.dao.ReportDao;
import com.skillswap.db.Database;
import com.skillswap.exception.DataAccessException;
import com.skillswap.model.Report;
import com.skillswap.model.enums.ReportStatus;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * JDBC implementation of ReportDao for handling moderation complaints and reports.
 */
public class JdbcReportDao implements ReportDao {

    @Override
    public long insert(Report r) {
        String sql = "INSERT INTO reports (reporter_id, target_user_id, target_review_id, reason, status, created_at) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection con = Database.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, r.getReporterId());
            if (r.getTargetUserId() != null) {
                ps.setLong(2, r.getTargetUserId());
            } else {
                ps.setNull(2, Types.BIGINT);
            }
            if (r.getTargetReviewId() != null) {
                ps.setLong(3, r.getTargetReviewId());
            } else {
                ps.setNull(3, Types.BIGINT);
            }
            ps.setString(4, r.getReason());
            ps.setString(5, r.getStatus().name());
            LocalDateTime created = r.getCreatedAt() != null ? r.getCreatedAt() : LocalDateTime.now();
            ps.setTimestamp(6, Timestamp.valueOf(created));

            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    r.setId(rs.getLong(1));
                    return r.getId();
                }
            }
            throw new DataAccessException("Failed to retrieve generated id for report");
        } catch (SQLException ex) {
            throw new DataAccessException("Failed to insert report", ex);
        }
    }

    @Override
    public List<Report> findByStatus(ReportStatus s, int limit, int offset) {
        StringBuilder sql = new StringBuilder(
                "SELECT r.id, r.reporter_id, r.target_user_id, r.target_review_id, r.reason, "
                        + "r.status, r.created_at, r.resolved_by, r.resolved_at, "
                        + "ur.full_name AS reporter_name, ut.full_name AS target_name "
                        + "FROM reports r "
                        + "JOIN users ur ON r.reporter_id = ur.id "
                        + "LEFT JOIN users ut ON r.target_user_id = ut.id "
        );
        if (s != null) {
            sql.append("WHERE r.status = ? ");
        }
        sql.append("ORDER BY r.created_at DESC LIMIT ? OFFSET ?");

        List<Report> list = new ArrayList<>();
        try (Connection con = Database.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql.toString())) {
            int p = 1;
            if (s != null) {
                ps.setString(p++, s.name());
            }
            ps.setInt(p++, limit);
            ps.setInt(p, offset);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
            return list;
        } catch (SQLException ex) {
            throw new DataAccessException("Failed to find reports by status", ex);
        }
    }

    @Override
    public void resolve(long id, long adminId, ReportStatus s) {
        String sql = "UPDATE reports SET status = ?, resolved_by = ?, resolved_at = NOW() WHERE id = ?";
        try (Connection con = Database.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, s.name());
            ps.setLong(2, adminId);
            ps.setLong(3, id);
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new DataAccessException("Failed to resolve report " + id, ex);
        }
    }

    @Override
    public int countByStatus(ReportStatus s) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM reports ");
        if (s != null) {
            sql.append("WHERE status = ?");
        }
        try (Connection con = Database.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql.toString())) {
            if (s != null) {
                ps.setString(1, s.name());
            }
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
            return 0;
        } catch (SQLException ex) {
            throw new DataAccessException("Failed to count reports by status", ex);
        }
    }

    private Report mapRow(ResultSet rs) throws SQLException {
        Report r = new Report();
        r.setId(rs.getLong("id"));
        r.setReporterId(rs.getLong("reporter_id"));
        long targetUid = rs.getLong("target_user_id");
        if (!rs.wasNull()) {
            r.setTargetUserId(targetUid);
        }
        long targetRevId = rs.getLong("target_review_id");
        if (!rs.wasNull()) {
            r.setTargetReviewId(targetRevId);
        }
        r.setReason(rs.getString("reason"));
        r.setStatus(ReportStatus.valueOf(rs.getString("status")));
        Timestamp createdTs = rs.getTimestamp("created_at");
        if (createdTs != null) {
            r.setCreatedAt(createdTs.toLocalDateTime());
        }
        long resBy = rs.getLong("resolved_by");
        if (!rs.wasNull()) {
            r.setResolvedBy(resBy);
        }
        Timestamp resAt = rs.getTimestamp("resolved_at");
        if (resAt != null) {
            r.setResolvedAt(resAt.toLocalDateTime());
        }
        r.setReporterName(rs.getString("reporter_name"));
        r.setTargetName(rs.getString("target_name"));
        return r;
    }
}
