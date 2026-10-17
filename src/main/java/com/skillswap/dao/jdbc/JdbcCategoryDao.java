package com.skillswap.dao.jdbc;

import com.skillswap.dao.CategoryDao;
import com.skillswap.db.Database;
import com.skillswap.exception.DataAccessException;
import com.skillswap.model.Category;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC implementation of CategoryDao.
 */
public class JdbcCategoryDao implements CategoryDao {

    @Override
    public List<Category> findAll() {
        String sql = "SELECT * FROM categories ORDER BY name ASC";
        List<Category> list = new ArrayList<>();
        try (Connection conn = Database.acquire();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new DataAccessException("Error finding all categories", e);
        }
    }

    @Override
    public long insert(Category c) {
        String sql = "INSERT INTO categories (name, description) VALUES (?, ?)";
        try (Connection conn = Database.acquire();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, c.getName());
            ps.setString(2, c.getDescription());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    c.setId(id);
                    return id;
                }
                throw new DataAccessException("Failed to retrieve generated key for category");
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error inserting category: " + c.getName(), e);
        }
    }

    @Override
    public void update(Category c) {
        String sql = "UPDATE categories SET name = ?, description = ? WHERE id = ?";
        try (Connection conn = Database.acquire();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, c.getName());
            ps.setString(2, c.getDescription());
            ps.setInt(3, c.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Error updating category: " + c.getId(), e);
        }
    }

    @Override
    public void delete(int id) {
        String sql = "DELETE FROM categories WHERE id = ?";
        try (Connection conn = Database.acquire();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Error deleting category: " + id, e);
        }
    }

    @Override
    public Optional<Category> findByName(String name) {
        String sql = "SELECT * FROM categories WHERE name = ?";
        try (Connection conn = Database.acquire();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error finding category by name: " + name, e);
        }
    }

    @Override
    public Optional<Category> findById(int id) {
        String sql = "SELECT * FROM categories WHERE id = ?";
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
            throw new DataAccessException("Error finding category by id: " + id, e);
        }
    }

    private Category mapRow(ResultSet rs) throws SQLException {
        Category c = new Category();
        c.setId(rs.getInt("id"));
        c.setName(rs.getString("name"));
        c.setDescription(rs.getString("description"));
        return c;
    }
}
