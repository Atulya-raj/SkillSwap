package com.skillswap.service;

import com.skillswap.dao.AvailabilityDao;
import com.skillswap.dao.BadgeDao;
import com.skillswap.dao.ListingDao;
import com.skillswap.dao.ReviewDao;
import com.skillswap.dao.UserDao;
import com.skillswap.dao.jdbc.JdbcAvailabilityDao;
import com.skillswap.dao.jdbc.JdbcBadgeDao;
import com.skillswap.dao.jdbc.JdbcListingDao;
import com.skillswap.dao.jdbc.JdbcReviewDao;
import com.skillswap.dao.jdbc.JdbcUserDao;
import com.skillswap.exception.ResourceNotFoundException;
import com.skillswap.exception.ValidationException;
import com.skillswap.io.FileStorage;
import com.skillswap.model.AvailabilitySlot;
import com.skillswap.model.Badge;
import com.skillswap.model.OfferedSkill;
import com.skillswap.model.Review;
import com.skillswap.model.User;
import com.skillswap.model.WantedSkill;
import com.skillswap.util.Validator;
import jakarta.servlet.http.Part;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service managing user profiles, public profile aggregation, and avatar uploads.
 */
public class ProfileService {

    private static final ProfileService INSTANCE = new ProfileService();

    private final UserDao userDao;
    private final ListingDao listingDao;
    private final AvailabilityDao availabilityDao;
    private final ReviewDao reviewDao;
    private final BadgeDao badgeDao;
    private final FileStorage fileStorage;

    public ProfileService() {
        this(new JdbcUserDao(), new JdbcListingDao(), new JdbcAvailabilityDao(),
                new JdbcReviewDao(), new JdbcBadgeDao(), FileStorage.getInstance());
    }

    public ProfileService(UserDao userDao, ListingDao listingDao, AvailabilityDao availabilityDao,
                          ReviewDao reviewDao, BadgeDao badgeDao, FileStorage fileStorage) {
        this.userDao = userDao;
        this.listingDao = listingDao;
        this.availabilityDao = availabilityDao;
        this.reviewDao = reviewDao;
        this.badgeDao = badgeDao;
        this.fileStorage = fileStorage;
    }

    /**
     * Returns the singleton instance of ProfileService.
     *
     * @return profile service singleton
     */
    public static ProfileService getInstance() {
        return INSTANCE;
    }

    /**
     * Retrieves user by id.
     *
     * @param userId user id
     * @return user entity
     * @throws ResourceNotFoundException if user is not found
     */
    public User get(long userId) throws ResourceNotFoundException {
        return userDao.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
    }

    /**
     * Updates profile fields for an existing user.
     *
     * @param userId user id
     * @param fullName full name
     * @param department department
     * @param studyYear study year (1-5)
     * @param bio user biography
     * @return updated user entity
     * @throws ResourceNotFoundException if user not found
     * @throws ValidationException if profile input fails validation
     */
    public User update(long userId, String fullName, String department, Integer studyYear, String bio)
            throws ResourceNotFoundException, ValidationException {
        Validator.validateProfile(fullName, department, studyYear, bio);

        User user = get(userId);
        user.setFullName(fullName.trim());
        user.setDepartment(department != null && !department.trim().isEmpty() ? department.trim() : null);
        user.setStudyYear(studyYear);
        user.setBio(bio != null && !bio.trim().isEmpty() ? bio.trim() : null);

        userDao.update(user);
        return user;
    }

    /**
     * Saves an uploaded avatar image part for the given user.
     *
     * @param userId user id
     * @param avatarPart multipart file part
     * @return saved avatar filename
     * @throws ResourceNotFoundException if user not found
     * @throws ValidationException if image fails validation
     * @throws IOException on stream I/O failure
     */
    public String saveAvatar(long userId, Part avatarPart)
            throws ResourceNotFoundException, ValidationException, IOException {
        if (avatarPart == null || avatarPart.getSize() <= 0) {
            Map<String, String> errors = new HashMap<>();
            errors.put("avatar", "Please select an image file to upload");
            throw new ValidationException(errors);
        }

        User user = get(userId);
        String submittedFilename = avatarPart.getSubmittedFileName();
        if (submittedFilename == null || submittedFilename.trim().isEmpty()) {
            submittedFilename = "avatar.jpg";
        }

        try (InputStream in = avatarPart.getInputStream()) {
            String newFilename = fileStorage.saveAvatar(
                    submittedFilename, in, avatarPart.getSize(), user.getAvatarPath()
            );
            userDao.updateAvatar(userId, newFilename);
            user.setAvatarPath(newFilename);
            return newFilename;
        }
    }

    /**
     * Loads complete public profile details for a user.
     *
     * @param userId user id
     * @return aggregated public profile view data
     * @throws ResourceNotFoundException if user not found
     */
    public PublicProfileData getPublicProfile(long userId) throws ResourceNotFoundException {
        User user = get(userId);
        List<OfferedSkill> offered = listingDao.findOfferedByUser(userId);
        List<WantedSkill> wanted = listingDao.findWantedByUser(userId);
        List<AvailabilitySlot> availability = availabilityDao.findByUser(userId);
        List<Review> reviews = reviewDao.findForUser(userId, 20);
        List<Badge> badges = badgeDao.findForUser(userId);

        return new PublicProfileData(user, offered, wanted, availability, reviews, badges);
    }

    /**
     * Data transfer container for public profile views.
     */
    public static class PublicProfileData {
        private final User user;
        private final List<OfferedSkill> offeredSkills;
        private final List<WantedSkill> wantedSkills;
        private final List<AvailabilitySlot> availabilitySlots;
        private final List<Review> reviews;
        private final List<Badge> badges;

        public PublicProfileData(User user, List<OfferedSkill> offeredSkills, List<WantedSkill> wantedSkills,
                                 List<AvailabilitySlot> availabilitySlots, List<Review> reviews, List<Badge> badges) {
            this.user = user;
            this.offeredSkills = offeredSkills;
            this.wantedSkills = wantedSkills;
            this.availabilitySlots = availabilitySlots;
            this.reviews = reviews;
            this.badges = badges;
        }

        public User getUser() {
            return user;
        }

        public List<OfferedSkill> getOfferedSkills() {
            return offeredSkills;
        }

        public List<WantedSkill> getWantedSkills() {
            return wantedSkills;
        }

        public List<AvailabilitySlot> getAvailabilitySlots() {
            return availabilitySlots;
        }

        public List<Review> getReviews() {
            return reviews;
        }

        public List<Badge> getBadges() {
            return badges;
        }
    }
}
