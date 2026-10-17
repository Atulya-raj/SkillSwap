package com.skillswap.matching;

import com.skillswap.dao.AvailabilityDao;
import com.skillswap.dao.ListingDao;
import com.skillswap.dao.SkillDao;
import com.skillswap.dao.UserDao;
import com.skillswap.dao.jdbc.JdbcAvailabilityDao;
import com.skillswap.dao.jdbc.JdbcListingDao;
import com.skillswap.dao.jdbc.JdbcSkillDao;
import com.skillswap.dao.jdbc.JdbcUserDao;
import com.skillswap.exception.NoMatchFoundException;
import com.skillswap.model.MatchResult;
import com.skillswap.model.OfferedSkill;
import com.skillswap.model.Skill;
import com.skillswap.model.User;
import com.skillswap.model.WantedSkill;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.PriorityQueue;

/**
 * Alternative matching engine prioritizing provider average rating first, then lowest price.
 */
public class RatingFirstMatchEngine implements MatchEngine {

    private final ListingDao listingDao;
    private final UserDao userDao;
    private final SkillDao skillDao;
    private final AvailabilityDao availabilityDao;

    // Comparator ordering best candidates last for min-heap polling:
    // lowest rating first, then highest price first
    private static final Comparator<MatchResult> HEAP_COMPARATOR = (a, b) -> {
        int ratingCmp = a.getProvider().getAvgRating().compareTo(b.getProvider().getAvgRating());
        if (ratingCmp != 0) {
            return ratingCmp; // smaller rating is polled first
        }
        return Integer.compare(b.getListing().getCreditsPerHour(), a.getListing().getCreditsPerHour()); // higher price is polled first
    };

    // Final sorting: highest rating first, then lowest price
    private static final Comparator<MatchResult> FINAL_COMPARATOR = (a, b) -> {
        int ratingCmp = b.getProvider().getAvgRating().compareTo(a.getProvider().getAvgRating());
        if (ratingCmp != 0) {
            return ratingCmp;
        }
        return Integer.compare(a.getListing().getCreditsPerHour(), b.getListing().getCreditsPerHour());
    };

    public RatingFirstMatchEngine() {
        this(new JdbcListingDao(), new JdbcUserDao(), new JdbcSkillDao(), new JdbcAvailabilityDao());
    }

    public RatingFirstMatchEngine(ListingDao listingDao, UserDao userDao, SkillDao skillDao, AvailabilityDao availabilityDao) {
        this.listingDao = listingDao;
        this.userDao = userDao;
        this.skillDao = skillDao;
        this.availabilityDao = availabilityDao;
    }

    @Override
    public List<MatchResult> findProviders(long learnerId, long skillId, int topK) throws NoMatchFoundException {
        if (topK <= 0) {
            topK = 10;
        }

        Optional<Skill> targetSkillOpt = skillDao.findById((int) skillId);
        int targetCategoryId = targetSkillOpt.map(Skill::getCategoryId).orElse(0);

        List<OfferedSkill> candidates = new ArrayList<>();
        candidates.addAll(listingDao.findOfferedBySkill((int) skillId));
        if (targetCategoryId > 0) {
            for (OfferedSkill sameCat : listingDao.findOfferedByCategory(targetCategoryId)) {
                if (sameCat.getSkillId() != skillId) {
                    candidates.add(sameCat);
                }
            }
        }

        PriorityQueue<MatchResult> minHeap = new PriorityQueue<>(topK, HEAP_COMPARATOR);
        Map<Long, User> userCache = new HashMap<>();

        for (OfferedSkill listing : candidates) {
            if (!listing.isActive() || listing.getUserId() == learnerId) {
                continue;
            }

            User provider = userCache.computeIfAbsent(listing.getUserId(), id -> userDao.findById(id).orElse(null));
            if (provider == null || !provider.isActive()) {
                continue;
            }

            double relevance = (listing.getSkillId() == skillId) ? 1.0 : 0.4;
            double ratingScore = Math.min(1.0, provider.getAvgRating().doubleValue() / 5.0);
            double score = ratingScore; // Score represents rating in this engine

            MatchResult mr = new MatchResult(provider, listing, score, relevance, ratingScore, 0.0);

            if (minHeap.size() < topK) {
                minHeap.offer(mr);
            } else if (HEAP_COMPARATOR.compare(mr, minHeap.peek()) > 0) {
                minHeap.poll();
                minHeap.offer(mr);
            }
        }

        if (minHeap.isEmpty()) {
            throw new NoMatchFoundException("No matching providers found for this skill.");
        }

        List<MatchResult> result = new ArrayList<>(minHeap);
        result.sort(FINAL_COMPARATOR);
        return result;
    }

    @Override
    public List<MatchResult> recommendFor(long learnerId, int topK) {
        if (topK <= 0) {
            topK = 10;
        }

        List<WantedSkill> wanted = listingDao.findWantedByUser(learnerId);
        PriorityQueue<MatchResult> minHeap = new PriorityQueue<>(topK, HEAP_COMPARATOR);
        Map<Long, MatchResult> bestPerListing = new HashMap<>();

        if (!wanted.isEmpty()) {
            for (WantedSkill ws : wanted) {
                try {
                    List<MatchResult> matches = findProviders(learnerId, ws.getSkillId(), topK);
                    for (MatchResult mr : matches) {
                        long listingId = mr.getListing().getId();
                        if (!bestPerListing.containsKey(listingId)
                                || FINAL_COMPARATOR.compare(mr, bestPerListing.get(listingId)) < 0) {
                            bestPerListing.put(listingId, mr);
                        }
                    }
                } catch (NoMatchFoundException ignored) {
                }
            }
        } else {
            List<Skill> allSkills = skillDao.findAll();
            for (Skill s : allSkills) {
                try {
                    List<MatchResult> matches = findProviders(learnerId, s.getId(), 3);
                    for (MatchResult mr : matches) {
                        long listingId = mr.getListing().getId();
                        if (!bestPerListing.containsKey(listingId)) {
                            bestPerListing.put(listingId, mr);
                        }
                    }
                } catch (NoMatchFoundException ignored) {
                }
            }
        }

        for (MatchResult mr : bestPerListing.values()) {
            if (minHeap.size() < topK) {
                minHeap.offer(mr);
            } else if (HEAP_COMPARATOR.compare(mr, minHeap.peek()) > 0) {
                minHeap.poll();
                minHeap.offer(mr);
            }
        }

        List<MatchResult> result = new ArrayList<>(minHeap);
        result.sort(FINAL_COMPARATOR);
        return result;
    }
}
