package com.skillswap.dao.jdbc;

import com.skillswap.dao.UserDao;
import com.skillswap.db.Database;
import com.skillswap.exception.DataAccessException;
import com.skillswap.model.Admin;
import com.skillswap.model.Student;
import com.skillswap.model.User;
import com.skillswap.model.enums.Role;

import java.math.BigDecimal;
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
 * JDBC implementation of UserDao.
 */
public class JdbcUserDao implements UserDao {

    @Override
    public long insert(User u) {
        String sql = "INSERT INTO users (full_name, email, password_hash, role, department, study_year, bio, avatar_path, credits, is_active) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = Database.acquire();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, u.getFullName());
            ps.setString(2, u.getEmail());
            ps.setString(3, u.getPasswordHash());
            ps.setString(4, u.getRole() != null ? u.getRole().name() : Role.STUDENT.name());
            ps.setString(5, u.getDepartment());
            if (u.getStudyYear() != null) {
                ps.setInt(6, u.getStudyYear());
            } else {
                ps.setNull(6, java.sql.Types.TINYINT);
            }
            ps.setString(7, u.getBio());
            ps.setString(8, u.getAvatarPath());
            ps.setInt(9, u.getCredits());
            ps.setBoolean(10, u.isActive());

            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    long id = rs.getLong(1);
                    u.setId(id);
                    return id;
                }
                throw new DataAccessException("Failed to retrieve generated key for user");
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error inserting user: " + u.getEmail(), e);
        }
    }

    @Override
    public Optional<User> findById(long id) {
        String sql = "SELECT * FROM users WHERE id = ?";
        try (Connection conn = Database.acquire();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error finding user by id: " + id, e);
        }
    }

    @Override
    public Optional<User> findByEmail(String email) {
        String sql = "SELECT * FROM users WHERE email = ?";
        try (Connection conn = Database.acquire();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email != null ? email.trim() : null);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error finding user by email: " + email, e);
        }
    }

    @Override
    public boolean emailExists(String email) {
        String sql = "SELECT 1 FROM users WHERE email = ?";
        try (Connection conn = Database.acquire();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email != null ? email.trim() : null);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error checking email existence: " + email, e);
        }
    }

    @Override
    public void update(User u) {
        String sql = "UPDATE users SET full_name = ?, department = ?, study_year = ?, bio = ? WHERE id = ?";
        try (Connection conn = Database.acquire();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, u.getFullName());
            ps.setString(2, u.getDepartment());
            if (u.getStudyYear() != null) {
                ps.setInt(3, u.getStudyYear());
            } else {
                ps.setNull(3, java.sql.Types.TINYINT);
            }
            ps.setString(4, u.getBio());
            ps.setLong(5, u.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Error updating user: " + u.getId(), e);
        }
    }

    @Override
    public void updatePassword(long id, String hash) {
        String sql = "UPDATE users SET password_hash = ? WHERE id = ?";
        try (Connection conn = Database.acquire();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, hash);
            ps.setLong(2, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Error updating password for user: " + id, e);
        }
    }

    @Override
    public void updateAvatar(long id, String path) {
        String sql = "UPDATE users SET avatar_path = ? WHERE id = ?";
        try (Connection conn = Database.acquire();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, path);
            ps.setLong(2, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Error updating avatar for user: " + id, e);
        }
    }

    @Override
    public void touchLogin(long id) {
        String sql = "UPDATE users SET last_login_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (Connection conn = Database.acquire();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Error updating last_login_at for user: " + id, e);
        }
    }

    @Override
    public void setActive(long id, boolean active) {
        String sql = "UPDATE users SET is_active = ? WHERE id = ?";
        try (Connection conn = Database.acquire();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBoolean(1, active);
            ps.setLong(2, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Error setting active status for user: " + id, e);
        }
    }

    @Override
    public User lockForUpdate(long id) {
        String sql = "SELECT * FROM users WHERE id = ? FOR UPDATE";
        try (Connection conn = Database.acquire();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
                throw new DataAccessException("User not found for locking with id: " + id);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error locking user for update: " + id, e);
        }
    }

    @Override
    public void setCredits(long id, int newBalance) {
        String sql = "UPDATE users SET credits = ? WHERE id = ?";
        try (Connection conn = Database.acquire();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, newBalance);
            ps.setLong(2, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Error setting credits for user: " + id, e);
        }
    }

    @Override
    public void applyRatingStats(long id, BigDecimal avg, int count) {
        String sql = "UPDATE users SET avg_rating = ?, rating_count = ? WHERE id = ?";
        try (Connection conn = Database.acquire();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBigDecimal(1, avg != null ? avg : BigDecimal.ZERO);
            ps.setInt(2, count);
            ps.setLong(3, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Error applying rating stats for user: " + id, e);
        }
    }

    @Override
    public void incrementTaught(long id) {
        String sql = "UPDATE users SET sessions_taught = sessions_taught + 1 WHERE id = ?";
        try (Connection conn = Database.acquire();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Error incrementing sessions_taught for user: " + id, e);
        }
    }

    @Override
    public void incrementLearned(long id) {
        String sql = "UPDATE users SET sessions_learned = sessions_learned + 1 WHERE id = ?";
        try (Connection conn = Database.acquire();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Error incrementing sessions_learned for user: " + id, e);
        }
    }

    @Override
    public void incrementStrikes(long id) {
        String sql = "UPDATE users SET reliability_strikes = reliability_strikes + 1 WHERE id = ?";
        try (Connection conn = Database.acquire();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Error incrementing reliability strikes for user: " + id, e);
        }
    }

    @Override
    public List<User> search(String q, int limit, int offset) {
        String query = q != null ? "%" + q.trim() + "%" : "%";
        String sql = "SELECT * FROM users WHERE full_name LIKE ? OR email LIKE ? OR department LIKE ? " +
                "ORDER BY id DESC LIMIT ? OFFSET ?";
        List<User> list = new ArrayList<>();
        try (Connection conn = Database.acquire();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, query);
            ps.setString(2, query);
            ps.setString(3, query);
            ps.setInt(4, limit);
            ps.setInt(5, offset);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new DataAccessException("Error searching users", e);
        }
    }

    @Override
    public int count(String q) {
        String query = q != null ? "%" + q.trim() + "%" : "%";
        String sql = "SELECT COUNT(*) FROM users WHERE full_name LIKE ? OR email LIKE ? OR department LIKE ?";
        try (Connection conn = Database.acquire();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, query);
            ps.setString(2, query);
            ps.setString(3, query);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
                return 0;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error counting users", e);
        }
    }

    @Override
    public List<User> topMentorsByRating(int minReviews, int limit) {
        String sql = "SELECT * FROM users WHERE role = 'STUDENT' AND is_active = TRUE AND rating_count >= ? " +
                "ORDER BY avg_rating DESC, rating_count DESC, id ASC LIMIT ?";
        List<User> list = new ArrayList<>();
        try (Connection conn = Database.acquire();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, minReviews);
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new DataAccessException("Error retrieving top mentors by rating", e);
        }
    }

    @Override
    public List<User> topMentorsBySessions(int limit) {
        String sql = "SELECT * FROM users WHERE role = 'STUDENT' AND is_active = TRUE AND sessions_taught > 0 " +
                "ORDER BY sessions_taught DESC, avg_rating DESC, id ASC LIMIT ?";
        List<User> list = new ArrayList<>();
        try (Connection conn = Database.acquire();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new DataAccessException("Error retrieving top mentors by sessions", e);
        }
    }

    @Override
    public List<User> findAll(int limit, int offset) {
        String sql = "SELECT * FROM users ORDER BY id DESC LIMIT ? OFFSET ?";
        List<User> list = new ArrayList<>();
        try (Connection conn = Database.acquire();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            ps.setInt(2, offset);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new DataAccessException("Error listing all users", e);
        }
    }

    private User mapRow(ResultSet rs) throws SQLException {
        String roleStr = rs.getString("role");
        Role role = Role.valueOf(roleStr);
        User u = (role == Role.ADMIN) ? new Admin() : new Student();

        u.setId(rs.getLong("id"));
        u.setFullName(rs.getString("full_name"));
        u.setEmail(rs.getString("email"));
        u.setPasswordHash(rs.getString("password_hash"));
        u.setRole(role);
        u.setDepartment(rs.getString("department"));
        int studyYear = rs.getInt("study_year");
        if (!rs.wasNull()) {
            u.setStudyYear(studyYear);
        }
        u.setBio(rs.getString("bio"));
        u.setAvatarPath(rs.getString("avatar_path"));
        u.setCredits(rs.getInt("credits"));
        u.setAvgRating(rs.getBigDecimal("avg_rating"));
        u.setRatingCount(rs.getInt("rating_count"));
        u.setSessionsTaught(rs.getInt("sessions_taught"));
        u.setSessionsLearned(rs.getInt("sessions_learned"));
        u.setReliabilityStrikes(rs.getInt("reliability_strikes"));
        u.setActive(rs.getBoolean("is_active"));

        Timestamp createdTs = rs.getTimestamp("created_at");
        if (createdTs != null) {
            u.setCreatedAt(createdTs.toLocalDateTime());
        }
        Timestamp loginTs = rs.getTimestamp("last_login_at");
        if (loginTs != null) {
            u.setLastLoginAt(loginTs.toLocalDateTime());
        }

        return u;
    }
}
