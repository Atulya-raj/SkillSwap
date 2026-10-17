package com.skillswap.dao.jdbc;

import com.skillswap.dao.CreditDao;
import com.skillswap.db.Database;
import com.skillswap.exception.DataAccessException;
import com.skillswap.model.CreditEntry;
import com.skillswap.model.enums.LedgerType;

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
 * JDBC implementation of CreditDao for ledger entries and circulation balances.
 */
public class JdbcCreditDao implements CreditDao {

    @Override
    public long insert(CreditEntry e) {
        String sql = "INSERT INTO credits_ledger (user_id, session_request_id, entry_type, amount, balance_after, note, created_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection con = Database.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, e.getUserId());
            if (e.getSessionRequestId() != null) {
                ps.setLong(2, e.getSessionRequestId());
            } else {
                ps.setNull(2, Types.BIGINT);
            }
            ps.setString(3, e.getEntryType().name());
            ps.setInt(4, e.getAmount());
            ps.setInt(5, e.getBalanceAfter());
            ps.setString(6, e.getNote());
            LocalDateTime created = e.getCreatedAt() != null ? e.getCreatedAt() : LocalDateTime.now();
            ps.setTimestamp(7, Timestamp.valueOf(created));

            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    e.setId(rs.getLong(1));
                    return e.getId();
                }
            }
            throw new DataAccessException("Failed to retrieve generated id for credit entry");
        } catch (SQLException ex) {
            throw new DataAccessException("Failed to insert credit entry", ex);
        }
    }

    @Override
    public List<CreditEntry> findByUser(long userId, int limit, int offset) {
        String sql = "SELECT id, user_id, session_request_id, entry_type, amount, balance_after, note, created_at "
                + "FROM credits_ledger WHERE user_id = ? ORDER BY created_at DESC, id DESC LIMIT ? OFFSET ?";
        List<CreditEntry> list = new ArrayList<>();
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
            throw new DataAccessException("Failed to find credit ledger entries for user " + userId, ex);
        }
    }

    @Override
    public int countByUser(long userId) {
        String sql = "SELECT COUNT(*) FROM credits_ledger WHERE user_id = ?";
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
            throw new DataAccessException("Failed to count credit entries for user " + userId, ex);
        }
    }

    @Override
    public int sumByType(long userId, LedgerType t) {
        String sql = "SELECT COALESCE(SUM(amount), 0) FROM credits_ledger WHERE user_id = ? AND entry_type = ?";
        try (Connection con = Database.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setString(2, t.name());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
            return 0;
        } catch (SQLException ex) {
            throw new DataAccessException("Failed to sum credit entries by type", ex);
        }
    }

    @Override
    public int totalCreditsInCirculation() {
        String sql = "SELECT COALESCE(SUM(credits), 0) FROM users WHERE role = 'STUDENT' AND is_active = TRUE";
        try (Connection con = Database.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
            return 0;
        } catch (SQLException ex) {
            throw new DataAccessException("Failed to calculate total credits in circulation", ex);
        }
    }

    @Override
    public int sumHeldCredits(long userId) {
        String sql = "SELECT COALESCE(SUM(cost_credits), 0) FROM session_requests "
                + "WHERE learner_id = ? AND status IN ('PENDING', 'ACCEPTED')";
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
            throw new DataAccessException("Failed to sum held credits for user " + userId, ex);
        }
    }

    private CreditEntry mapRow(ResultSet rs) throws SQLException {
        CreditEntry e = new CreditEntry();
        e.setId(rs.getLong("id"));
        e.setUserId(rs.getLong("user_id"));
        long srId = rs.getLong("session_request_id");
        if (!rs.wasNull()) {
            e.setSessionRequestId(srId);
        }
        e.setEntryType(LedgerType.valueOf(rs.getString("entry_type")));
        e.setAmount(rs.getInt("amount"));
        e.setBalanceAfter(rs.getInt("balance_after"));
        e.setNote(rs.getString("note"));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) {
            e.setCreatedAt(ts.toLocalDateTime());
        }
        return e;
    }
}
