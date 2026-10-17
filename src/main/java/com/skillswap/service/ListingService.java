package com.skillswap.service;

import com.skillswap.dao.ListingDao;
import com.skillswap.dao.jdbc.JdbcListingDao;
import com.skillswap.exception.AuthorizationException;
import com.skillswap.exception.ResourceNotFoundException;
import com.skillswap.exception.ValidationException;
import com.skillswap.model.OfferedSkill;
import com.skillswap.model.SkillListing;
import com.skillswap.model.WantedSkill;
import com.skillswap.model.enums.ListingType;
import com.skillswap.model.enums.SkillLevel;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service managing user skill listings (offered skills and wanted skills).
 */
public class ListingService {

    private static final ListingService INSTANCE = new ListingService();

    private final ListingDao listingDao;
    private final BadgeService badgeService;

    public ListingService() {
        this(new JdbcListingDao(), BadgeService.getInstance());
    }

    public ListingService(ListingDao listingDao, BadgeService badgeService) {
        this.listingDao = listingDao;
        this.badgeService = badgeService;
    }

    /**
     * Returns the singleton instance of ListingService.
     *
     * @return listing service singleton
     */
    public static ListingService getInstance() {
        return INSTANCE;
    }

    /**
     * Creates a new offered skill listing for a user.
     *
     * @param userId user id
     * @param skillId skill id
     * @param level skill proficiency level
     * @param creditsPerHour credits charged per hour (1-5)
     * @param description optional description
     * @return created listing id
     * @throws ValidationException if invalid or duplicate
     */
    public long offer(long userId, int skillId, SkillLevel level, int creditsPerHour, String description)
            throws ValidationException {
        OfferedSkill offered = new OfferedSkill();
        offered.setUserId(userId);
        offered.setSkillId(skillId);
        offered.setLevel(level);
        offered.setCreditsPerHour(creditsPerHour);
        offered.setDescription(description != null ? description.trim() : null);
        offered.setActive(true);

        offered.validate();

        if (listingDao.existsUserSkill(userId, skillId, ListingType.OFFERED)) {
            Map<String, String> errors = new HashMap<>();
            errors.put("skillId", "You are already offering this skill.");
            throw new ValidationException(errors);
        }

        long id = listingDao.insert(offered);
        badgeService.evaluate(userId);
        return id;
    }

    /**
     * Creates a new wanted skill listing for a user.
     *
     * @param userId user id
     * @param skillId skill id
     * @param level desired skill level
     * @param description optional notes
     * @return created listing id
     * @throws ValidationException if invalid or duplicate
     */
    public long want(long userId, int skillId, SkillLevel level, String description)
            throws ValidationException {
        WantedSkill wanted = new WantedSkill();
        wanted.setUserId(userId);
        wanted.setSkillId(skillId);
        wanted.setLevel(level);
        wanted.setDescription(description != null ? description.trim() : null);
        wanted.setActive(true);

        wanted.validate();

        if (listingDao.existsUserSkill(userId, skillId, ListingType.WANTED)) {
            Map<String, String> errors = new HashMap<>();
            errors.put("skillId", "You are already looking to learn this skill.");
            throw new ValidationException(errors);
        }

        long id = listingDao.insert(wanted);
        badgeService.evaluate(userId);
        return id;
    }

    /**
     * Removes an offered or wanted listing belonging to a user.
     *
     * @param userId user id
     * @param listingId listing id
     * @throws ResourceNotFoundException if listing does not exist
     * @throws AuthorizationException if listing belongs to someone else
     */
    public void remove(long userId, long listingId) throws ResourceNotFoundException, AuthorizationException {
        Optional<SkillListing> opt = listingDao.findById(listingId);
        if (opt.isEmpty()) {
            throw new ResourceNotFoundException("Listing not found with id: " + listingId);
        }
        SkillListing listing = opt.get();
        if (listing.getUserId() != userId) {
            throw new AuthorizationException("You are not authorized to remove this listing.");
        }

        listingDao.delete(listingId);
        badgeService.evaluate(userId);
    }

    /**
     * Returns all skill listings for a user.
     *
     * @param userId user id
     * @return list of listings
     */
    public List<SkillListing> listFor(long userId) {
        return listingDao.findByUser(userId);
    }

    /**
     * Returns offered skills for a user.
     *
     * @param userId user id
     * @return list of offered skills
     */
    public List<OfferedSkill> findOfferedByUser(long userId) {
        return listingDao.findOfferedByUser(userId);
    }

    /**
     * Returns wanted skills for a user.
     *
     * @param userId user id
     * @return list of wanted skills
     */
    public List<WantedSkill> findWantedByUser(long userId) {
        return listingDao.findWantedByUser(userId);
    }

    /**
     * Counts active offered skills by a user.
     *
     * @param userId user id
     * @return count of offered skills
     */
    public int countOfferedByUser(long userId) {
        return listingDao.countOfferedByUser(userId);
    }
}
