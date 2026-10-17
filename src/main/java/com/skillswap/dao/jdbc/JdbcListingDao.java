package com.skillswap.dao.jdbc;

import com.skillswap.dao.ListingDao;
import com.skillswap.dao.SearchCriteria;
import com.skillswap.db.Database;
import com.skillswap.exception.DataAccessException;
import com.skillswap.model.OfferedSkill;
import com.skillswap.model.SkillListing;
import com.skillswap.model.WantedSkill;
import com.skillswap.model.enums.ListingType;
import com.skillswap.model.enums.SkillLevel;

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
import java.util.Optional;

/**
 * JDBC implementation of ListingDao supporting polymorphic SkillListing persistence.
 */
public class JdbcListingDao implements ListingDao {

    @Override
    public long insert(SkillListing listing) {
        String sql = "INSERT INTO user_skills (user_id, skill_id, type, level, credits_per_hour, description, is_active) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = Database.acquire();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, listing.getUserId());
            ps.setInt(2, listing.getSkillId());
            ps.setString(3, listing.type().name());
            ps.setString(4, listing.getLevel().name());

            if (listing instanceof OfferedSkill) {
                ps.setInt(5, ((OfferedSkill) listing).getCreditsPerHour());
            } else {
                ps.setNull(5, Types.TINYINT);
            }
            ps.setString(6, listing.getDescription());
            ps.setBoolean(7, listing.isActive());

            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    long id = rs.getLong(1);
                    listing.setId(id);
                    return id;
                }
                throw new DataAccessException("Failed to retrieve generated key for user skill listing");
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error inserting skill listing", e);
        }
    }

    @Override
    public void setActive(long id, boolean active) {
        String sql = "UPDATE user_skills SET is_active = ? WHERE id = ?";
        try (Connection conn = Database.acquire();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBoolean(1, active);
            ps.setLong(2, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Error setting active status for listing: " + id, e);
        }
    }

    @Override
    public void delete(long id) {
        String sql = "DELETE FROM user_skills WHERE id = ?";
        try (Connection conn = Database.acquire();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Error deleting listing: " + id, e);
        }
    }

    @Override
    public List<OfferedSkill> findOfferedByUser(long userId) {
        String sql = "SELECT us.*, s.name AS skill_name, c.name AS category_name FROM user_skills us " +
                "JOIN skills s ON us.skill_id = s.id " +
                "JOIN categories c ON s.category_id = c.id " +
                "WHERE us.user_id = ? AND us.type = 'OFFERED' ORDER BY us.id DESC";
        List<OfferedSkill> list = new ArrayList<>();
        try (Connection conn = Database.acquire();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add((OfferedSkill) mapRow(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new DataAccessException("Error finding offered skills for user: " + userId, e);
        }
    }

    @Override
    public List<WantedSkill> findWantedByUser(long userId) {
        String sql = "SELECT us.*, s.name AS skill_name, c.name AS category_name FROM user_skills us " +
                "JOIN skills s ON us.skill_id = s.id " +
                "JOIN categories c ON s.category_id = c.id " +
                "WHERE us.user_id = ? AND us.type = 'WANTED' ORDER BY us.id DESC";
        List<WantedSkill> list = new ArrayList<>();
        try (Connection conn = Database.acquire();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add((WantedSkill) mapRow(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new DataAccessException("Error finding wanted skills for user: " + userId, e);
        }
    }

    @Override
    public Optional<OfferedSkill> findOfferedById(long id) {
        String sql = "SELECT us.*, s.name AS skill_name, c.name AS category_name FROM user_skills us " +
                "JOIN skills s ON us.skill_id = s.id " +
                "JOIN categories c ON s.category_id = c.id " +
                "WHERE us.id = ? AND us.type = 'OFFERED'";
        try (Connection conn = Database.acquire();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of((OfferedSkill) mapRow(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error finding offered skill by id: " + id, e);
        }
    }

    @Override
    public Optional<SkillListing> findById(long id) {
        String sql = "SELECT us.*, s.name AS skill_name, c.name AS category_name FROM user_skills us " +
                "JOIN skills s ON us.skill_id = s.id " +
                "JOIN categories c ON s.category_id = c.id " +
                "WHERE us.id = ?";
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
            throw new DataAccessException("Error finding skill listing by id: " + id, e);
        }
    }

    @Override
    public List<SkillListing> findByUser(long userId) {
        String sql = "SELECT us.*, s.name AS skill_name, c.name AS category_name FROM user_skills us " +
                "JOIN skills s ON us.skill_id = s.id " +
                "JOIN categories c ON s.category_id = c.id " +
                "WHERE us.user_id = ? ORDER BY us.id DESC";
        List<SkillListing> list = new ArrayList<>();
        try (Connection conn = Database.acquire();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new DataAccessException("Error finding skill listings for user: " + userId, e);
        }
    }

    @Override
    public List<OfferedSkill> findOfferedBySkill(int skillId) {
        String sql = "SELECT us.*, s.name AS skill_name, c.name AS category_name FROM user_skills us " +
                "JOIN skills s ON us.skill_id = s.id " +
                "JOIN categories c ON s.category_id = c.id " +
                "JOIN users u ON us.user_id = u.id " +
                "WHERE us.skill_id = ? AND us.type = 'OFFERED' AND us.is_active = TRUE AND u.is_active = TRUE " +
                "ORDER BY u.avg_rating DESC, us.credits_per_hour ASC";
        List<OfferedSkill> list = new ArrayList<>();
        try (Connection conn = Database.acquire();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, skillId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add((OfferedSkill) mapRow(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new DataAccessException("Error finding offered skills by skill id: " + skillId, e);
        }
    }

    @Override
    public List<OfferedSkill> findOfferedByCategory(int categoryId) {
        String sql = "SELECT us.*, s.name AS skill_name, c.name AS category_name FROM user_skills us " +
                "JOIN skills s ON us.skill_id = s.id " +
                "JOIN categories c ON s.category_id = c.id " +
                "JOIN users u ON us.user_id = u.id " +
                "WHERE s.category_id = ? AND us.type = 'OFFERED' AND us.is_active = TRUE AND u.is_active = TRUE " +
                "ORDER BY u.avg_rating DESC, us.credits_per_hour ASC";
        List<OfferedSkill> list = new ArrayList<>();
        try (Connection conn = Database.acquire();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, categoryId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add((OfferedSkill) mapRow(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new DataAccessException("Error finding offered skills by category id: " + categoryId, e);
        }
    }

    @Override
    public List<WantedSkill> findWantedCreatedSince(LocalDateTime since) {
        String sql = "SELECT us.*, s.name AS skill_name, c.name AS category_name FROM user_skills us " +
                "JOIN skills s ON us.skill_id = s.id " +
                "JOIN categories c ON s.category_id = c.id " +
                "WHERE us.type = 'WANTED' AND us.is_active = TRUE AND us.created_at >= ? " +
                "ORDER BY us.created_at ASC";
        List<WantedSkill> list = new ArrayList<>();
        try (Connection conn = Database.acquire();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(since));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add((WantedSkill) mapRow(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new DataAccessException("Error finding wanted skills created since: " + since, e);
        }
    }

    @Override
    public List<OfferedSkill> findOfferedCreatedSince(LocalDateTime since) {
        String sql = "SELECT us.*, s.name AS skill_name, c.name AS category_name FROM user_skills us " +
                "JOIN skills s ON us.skill_id = s.id " +
                "JOIN categories c ON s.category_id = c.id " +
                "WHERE us.type = 'OFFERED' AND us.is_active = TRUE AND us.created_at >= ? " +
                "ORDER BY us.created_at ASC";
        List<OfferedSkill> list = new ArrayList<>();
        try (Connection conn = Database.acquire();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(since));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add((OfferedSkill) mapRow(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new DataAccessException("Error finding offered skills created since: " + since, e);
        }
    }

    @Override
    public List<OfferedSkill> searchOffered(SearchCriteria criteria, int limit, int offset) {
        StringBuilder sql = new StringBuilder(
                "SELECT us.*, s.name AS skill_name, c.name AS category_name FROM user_skills us " +
                "JOIN skills s ON us.skill_id = s.id " +
                "JOIN categories c ON s.category_id = c.id " +
                "JOIN users u ON us.user_id = u.id " +
                "WHERE us.type = 'OFFERED' AND us.is_active = TRUE AND u.is_active = TRUE "
        );
        List<Object> params = buildSearchWhereClause(criteria, sql);

        // Sorting
        String sort = criteria != null && criteria.getSort() != null ? criteria.getSort().toUpperCase() : "BEST";
        switch (sort) {
            case "RATING" -> sql.append("ORDER BY u.avg_rating DESC, u.rating_count DESC, us.id DESC ");
            case "PRICE" -> sql.append("ORDER BY us.credits_per_hour ASC, u.avg_rating DESC, us.id DESC ");
            default -> sql.append("ORDER BY u.avg_rating DESC, u.rating_count DESC, us.id DESC ");
        }

        sql.append("LIMIT ? OFFSET ?");
        params.add(limit);
        params.add(offset);

        List<OfferedSkill> list = new ArrayList<>();
        try (Connection conn = Database.acquire();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add((OfferedSkill) mapRow(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new DataAccessException("Error searching offered skills", e);
        }
    }

    @Override
    public int countOffered(SearchCriteria criteria) {
        StringBuilder sql = new StringBuilder(
                "SELECT COUNT(*) FROM user_skills us " +
                "JOIN skills s ON us.skill_id = s.id " +
                "JOIN categories c ON s.category_id = c.id " +
                "JOIN users u ON us.user_id = u.id " +
                "WHERE us.type = 'OFFERED' AND us.is_active = TRUE AND u.is_active = TRUE "
        );
        List<Object> params = buildSearchWhereClause(criteria, sql);

        try (Connection conn = Database.acquire();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
                return 0;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error counting offered skills", e);
        }
    }

    private List<Object> buildSearchWhereClause(SearchCriteria criteria, StringBuilder sql) {
        List<Object> params = new ArrayList<>();
        if (criteria == null) {
            return params;
        }

        if (criteria.getKeyword() != null && !criteria.getKeyword().trim().isEmpty()) {
            String kw = "%" + criteria.getKeyword().trim() + "%";
            sql.append("AND (s.name LIKE ? OR us.description LIKE ? OR u.full_name LIKE ?) ");
            params.add(kw);
            params.add(kw);
            params.add(kw);
        }
        if (criteria.getCategoryId() != null && criteria.getCategoryId() > 0) {
            sql.append("AND s.category_id = ? ");
            params.add(criteria.getCategoryId());
        }
        if (criteria.getSkillId() != null && criteria.getSkillId() > 0) {
            sql.append("AND us.skill_id = ? ");
            params.add(criteria.getSkillId());
        }
        if (criteria.getLevel() != null) {
            sql.append("AND us.level = ? ");
            params.add(criteria.getLevel().name());
        }
        if (criteria.getMinRating() != null) {
            sql.append("AND u.avg_rating >= ? ");
            params.add(criteria.getMinRating());
        }
        if (criteria.getDayOfWeek() != null) {
            sql.append("AND EXISTS (SELECT 1 FROM availability_slots av WHERE av.user_id = u.id AND av.day_of_week = ?) ");
            params.add(criteria.getDayOfWeek().getValue());
        }

        return params;
    }

    @Override
    public int countOfferedByUser(long userId) {
        String sql = "SELECT COUNT(*) FROM user_skills WHERE user_id = ? AND type = 'OFFERED' AND is_active = TRUE";
        try (Connection conn = Database.acquire();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
                return 0;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error counting offered skills for user: " + userId, e);
        }
    }

    @Override
    public boolean existsUserSkill(long userId, int skillId, ListingType type) {
        String sql = "SELECT 1 FROM user_skills WHERE user_id = ? AND skill_id = ? AND type = ?";
        try (Connection conn = Database.acquire();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setInt(2, skillId);
            ps.setString(3, type.name());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error checking user skill existence", e);
        }
    }

    private SkillListing mapRow(ResultSet rs) throws SQLException {
        String typeStr = rs.getString("type");
        ListingType type = ListingType.valueOf(typeStr);

        SkillListing listing = (type == ListingType.OFFERED) ? new OfferedSkill() : new WantedSkill();
        listing.setId(rs.getLong("id"));
        listing.setUserId(rs.getLong("user_id"));
        listing.setSkillId(rs.getInt("skill_id"));
        listing.setLevel(SkillLevel.valueOf(rs.getString("level")));
        listing.setDescription(rs.getString("description"));
        listing.setActive(rs.getBoolean("is_active"));

        Timestamp createdTs = rs.getTimestamp("created_at");
        if (createdTs != null) {
            listing.setCreatedAt(createdTs.toLocalDateTime());
        }
        listing.setSkillName(rs.getString("skill_name"));
        listing.setCategoryName(rs.getString("category_name"));

        if (listing instanceof OfferedSkill) {
            ((OfferedSkill) listing).setCreditsPerHour(rs.getInt("credits_per_hour"));
        }

        return listing;
    }
}
