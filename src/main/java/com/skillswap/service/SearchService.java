package com.skillswap.service;

import com.skillswap.dao.BadgeDao;
import com.skillswap.dao.ListingDao;
import com.skillswap.dao.SearchCriteria;
import com.skillswap.dao.UserDao;
import com.skillswap.dao.jdbc.JdbcBadgeDao;
import com.skillswap.dao.jdbc.JdbcListingDao;
import com.skillswap.dao.jdbc.JdbcUserDao;
import com.skillswap.model.Badge;
import com.skillswap.model.OfferedSkill;
import com.skillswap.model.User;
import com.skillswap.util.Paginator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for browsing and filtering offered skill listings with pagination and provider cards.
 */
public class SearchService {

    private static final SearchService INSTANCE = new SearchService();
    private static final int DEFAULT_PAGE_SIZE = 10;

    private final ListingDao listingDao;
    private final UserDao userDao;
    private final BadgeDao badgeDao;

    public SearchService() {
        this(new JdbcListingDao(), new JdbcUserDao(), new JdbcBadgeDao());
    }

    public SearchService(ListingDao listingDao, UserDao userDao, BadgeDao badgeDao) {
        this.listingDao = listingDao;
        this.userDao = userDao;
        this.badgeDao = badgeDao;
    }

    /**
     * Returns the singleton instance of SearchService.
     *
     * @return search service singleton
     */
    public static SearchService getInstance() {
        return INSTANCE;
    }

    /**
     * Searches offered skill listings using the provided criteria and page number.
     *
     * @param criteria search criteria filters and sorting
     * @param page 1-indexed page number
     * @return paginated slice of provider cards
     */
    public Paginator.Page<ProviderCard> search(SearchCriteria criteria, int page) {
        return search(criteria, page, DEFAULT_PAGE_SIZE);
    }

    /**
     * Searches offered skill listings using the provided criteria, page, and page size.
     *
     * @param criteria search criteria
     * @param page 1-indexed page number
     * @param pageSize page size
     * @return paginated slice of provider cards
     */
    public Paginator.Page<ProviderCard> search(SearchCriteria criteria, int page, int pageSize) {
        int safePage = Math.max(1, page);
        int offset = Paginator.calculateOffset(safePage, pageSize);

        int totalItems = listingDao.countOffered(criteria);
        List<OfferedSkill> listings = listingDao.searchOffered(criteria, pageSize, offset);

        Map<Long, User> userCache = new HashMap<>();
        Map<Long, List<Badge>> badgeCache = new HashMap<>();
        List<ProviderCard> cards = new ArrayList<>();

        for (OfferedSkill listing : listings) {
            long uid = listing.getUserId();
            User provider = userCache.computeIfAbsent(uid, id -> userDao.findById(id).orElse(null));
            if (provider == null) {
                continue;
            }
            List<Badge> badges = badgeCache.computeIfAbsent(uid, id -> badgeDao.findForUser(id));
            cards.add(new ProviderCard(listing, provider, badges));
        }

        return new Paginator.Page<>(cards, safePage, pageSize, totalItems);
    }

    /**
     * View container representing an offered skill listing card on browse pages.
     */
    public static class ProviderCard {
        private final OfferedSkill listing;
        private final User provider;
        private final List<Badge> badges;

        public ProviderCard(OfferedSkill listing, User provider, List<Badge> badges) {
            this.listing = listing;
            this.provider = provider;
            this.badges = badges != null ? badges : List.of();
        }

        public OfferedSkill getListing() {
            return listing;
        }

        public User getProvider() {
            return provider;
        }

        public List<Badge> getBadges() {
            return badges;
        }
    }
}
