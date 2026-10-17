package com.skillswap.dao;

import com.skillswap.model.OfferedSkill;
import com.skillswap.model.SkillListing;
import com.skillswap.model.WantedSkill;
import com.skillswap.model.enums.ListingType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Data access interface for user skill listings (offered and wanted).
 */
public interface ListingDao {

    long insert(SkillListing listing);

    void setActive(long id, boolean active);

    void delete(long id);

    List<OfferedSkill> findOfferedByUser(long userId);

    List<WantedSkill> findWantedByUser(long userId);

    List<SkillListing> findByUser(long userId);

    Optional<OfferedSkill> findOfferedById(long id);

    Optional<SkillListing> findById(long id);

    List<OfferedSkill> findOfferedBySkill(int skillId);

    List<OfferedSkill> findOfferedByCategory(int categoryId);

    List<WantedSkill> findWantedCreatedSince(LocalDateTime since);

    List<OfferedSkill> findOfferedCreatedSince(LocalDateTime since);

    List<OfferedSkill> searchOffered(SearchCriteria criteria, int limit, int offset);

    int countOffered(SearchCriteria criteria);

    int countOfferedByUser(long userId);

    boolean existsUserSkill(long userId, int skillId, ListingType type);
}
