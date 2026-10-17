package com.skillswap.dao.jdbc;

import com.skillswap.dao.SkillDao;
import com.skillswap.db.Database;
import com.skillswap.exception.DataAccessException;
import com.skillswap.model.Skill;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC implementation of SkillDao.
 */
public class JdbcSkillDao implements SkillDao {

    @Override
    public List<Skill> findAll() {
        String sql = "SELECT s.*, c.name AS category_name FROM skills s " +
                "JOIN categories c ON s.category_id = c.id " +
                "ORDER BY c.name ASC, s.name ASC";
        List<Skill> list = new ArrayList<>();
        try (Connection conn = Database.acquire();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new DataAccessException("Error finding all skills", e);
        }
    }

    @Override
    public List<Skill> findByCategory(int categoryId) {
        String sql = "SELECT s.*, c.name AS category_name FROM skills s " +
                "JOIN categories c ON s.category_id = c.id " +
                "WHERE s.category_id = ? ORDER BY s.name ASC";
        List<Skill> list = new ArrayList<>();
        try (Connection conn = Database.acquire();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, categoryId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new DataAccessException("Error finding skills by category: " + categoryId, e);
        }
    }

    @Override
    public long insert(Skill s) {
        String sql = "INSERT INTO skills (category_id, name) VALUES (?, ?)";
        try (Connection conn = Database.acquire();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, s.getCategoryId());
            ps.setString(2, s.getName());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    s.setId(id);
                    return id;
                }
                throw new DataAccessException("Failed to retrieve generated key for skill");
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error inserting skill: " + s.getName(), e);
        }
    }

    @Override
    public void update(Skill s) {
        String sql = "UPDATE skills SET category_id = ?, name = ? WHERE id = ?";
        try (Connection conn = Database.acquire();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, s.getCategoryId());
            ps.setString(2, s.getName());
            ps.setInt(3, s.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Error updating skill: " + s.getId(), e);
        }
    }

    @Override
    public void delete(int id) {
        String sql = "DELETE FROM skills WHERE id = ?";
        try (Connection conn = Database.acquire();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Error deleting skill: " + id, e);
        }
    }

    @Override
    public Optional<Skill> findByCategoryAndName(int categoryId, String name) {
        String sql = "SELECT s.*, c.name AS category_name FROM skills s " +
                "JOIN categories c ON s.category_id = c.id " +
                "WHERE s.category_id = ? AND s.name = ?";
        try (Connection conn = Database.acquire();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, categoryId);
            ps.setString(2, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error finding skill by category and name", e);
        }
    }

    @Override
    public Optional<Skill> findById(int id) {
        String sql = "SELECT s.*, c.name AS category_name FROM skills s " +
                "JOIN categories c ON s.category_id = c.id " +
                "WHERE s.id = ?";
        try (Connection conn = Database.acquire();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error finding skill by id: " + id, e);
        }
    }

    private Skill mapRow(ResultSet rs) throws SQLException {
        Skill s = new Skill();
        s.setId(rs.getInt("id"));
        s.setCategoryId(rs.getInt("category_id"));
        s.setName(rs.getString("name"));
        s.setCategoryName(rs.getString("category_name"));
        return s;
    }
}
