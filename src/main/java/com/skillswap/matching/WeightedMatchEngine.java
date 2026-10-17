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
import com.skillswap.model.AvailabilitySlot;
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
 * Default matching engine using weighted multi-criteria scoring:
 * score = 0.5 * skillRelevance + 0.3 * ratingScore + 0.2 * availabilityOverlap.
 */
public class WeightedMatchEngine implements MatchEngine {

    private final ListingDao listingDao;
    private final UserDao userDao;
    private final SkillDao skillDao;
    private final AvailabilityDao availabilityDao;

    public WeightedMatchEngine() {
        this(new JdbcListingDao(), new JdbcUserDao(), new JdbcSkillDao(), new JdbcAvailabilityDao());
    }

    public WeightedMatchEngine(ListingDao listingDao, UserDao userDao, SkillDao skillDao, AvailabilityDao availabilityDao) {
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

        // Load all active offered listings
        List<OfferedSkill> candidates = new ArrayList<>();
        // Exact matches
        candidates.addAll(listingDao.findOfferedBySkill((int) skillId));
        // Same category matches
        if (targetCategoryId > 0) {
            for (OfferedSkill sameCat : listingDao.findOfferedByCategory(targetCategoryId)) {
                if (sameCat.getSkillId() != skillId) {
                    candidates.add(sameCat);
                }
            }
        }

        List<AvailabilitySlot> learnerSlots = availabilityDao.findByUser(learnerId);

        // Bounded Min-Heap: smallest score at the root
        PriorityQueue<MatchResult> minHeap = new PriorityQueue<>(
                topK,
                Comparator.comparingDouble(MatchResult::getScore)
        );

        Map<Long, User> userCache = new HashMap<>();
        Map<Long, List<AvailabilitySlot>> slotCache = new HashMap<>();

        for (OfferedSkill listing : candidates) {
            if (!listing.isActive()) {
                continue;
            }
            if (listing.getUserId() == learnerId) {
                continue; // Exclude the learner themselves
            }

            User provider = userCache.computeIfAbsent(listing.getUserId(), id -> userDao.findById(id).orElse(null));
            if (provider == null || !provider.isActive()) {
                continue; // Exclude inactive users
            }

            int listingCatId = listing.getCategoryId() > 0 ? listing.getCategoryId()
                    : skillDao.findById(listing.getSkillId()).map(Skill::getCategoryId).orElse(0);

            // 1. Skill Relevance
            double skillRelevance;
            if (listing.getSkillId() == skillId) {
                skillRelevance = 1.0;
            } else if (targetCategoryId > 0 && listingCatId == targetCategoryId) {
                skillRelevance = 0.4;
            } else {
                continue; // Excluded
            }

            // 2. Rating Score
            double ratingScore;
            if (provider.getRatingCount() < 3) {
                ratingScore = 0.6; // Cold-start neutral prior
            } else {
                ratingScore = Math.min(1.0, Math.max(0.0, provider.getAvgRating().doubleValue() / 5.0));
            }

            // 3. Availability Overlap
            List<AvailabilitySlot> providerSlots = slotCache.computeIfAbsent(
                    provider.getId(),
                    id -> availabilityDao.findByUser(id)
            );
            double availabilityOverlap = calculateAvailabilityOverlap(learnerSlots, providerSlots);

            // Combined Score
            double score = 0.5 * skillRelevance + 0.3 * ratingScore + 0.2 * availabilityOverlap;

            MatchResult mr = new MatchResult(provider, listing, score, skillRelevance, ratingScore, availabilityOverlap);

            if (minHeap.size() < topK) {
                minHeap.offer(mr);
            } else if (score > minHeap.peek().getScore()) {
                minHeap.poll();
                minHeap.offer(mr);
            }
        }

        if (minHeap.isEmpty()) {
            throw new NoMatchFoundException("No matching providers found for this skill.");
        }

        List<MatchResult> result = new ArrayList<>(minHeap);
        result.sort(Comparator.comparingDouble(MatchResult::getScore).reversed());
        return result;
    }

    @Override
    public List<MatchResult> recommendFor(long learnerId, int topK) {
        if (topK <= 0) {
            topK = 10;
        }

        List<WantedSkill> wanted = listingDao.findWantedByUser(learnerId);
        PriorityQueue<MatchResult> minHeap = new PriorityQueue<>(
                topK,
                Comparator.comparingDouble(MatchResult::getScore)
        );

        Map<Long, MatchResult> bestPerListing = new HashMap<>();

        if (!wanted.isEmpty()) {
            for (WantedSkill ws : wanted) {
                try {
                    List<MatchResult> matches = findProviders(learnerId, ws.getSkillId(), topK);
                    for (MatchResult mr : matches) {
                        long listingId = mr.getListing().getId();
                        if (!bestPerListing.containsKey(listingId)
                                || mr.getScore() > bestPerListing.get(listingId).getScore()) {
                            bestPerListing.put(listingId, mr);
                        }
                    }
                } catch (NoMatchFoundException ignored) {
                }
            }
        } else {
            // Learner has no wanted skills yet: discover top-rated active providers across categories
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
            } else if (mr.getScore() > minHeap.peek().getScore()) {
                minHeap.poll();
                minHeap.offer(mr);
            }
        }

        List<MatchResult> result = new ArrayList<>(minHeap);
        result.sort(Comparator.comparingDouble(MatchResult::getScore).reversed());
        return result;
    }

    private double calculateAvailabilityOverlap(List<AvailabilitySlot> learnerSlots, List<AvailabilitySlot> providerSlots) {
        if (learnerSlots == null || learnerSlots.isEmpty() || providerSlots == null || providerSlots.isEmpty()) {
            return 0.0;
        }

        int totalOverlapMinutes = 0;
        for (AvailabilitySlot ls : learnerSlots) {
            for (AvailabilitySlot ps : providerSlots) {
                totalOverlapMinutes += ls.overlapMinutes(ps);
            }
        }

        return Math.min(1.0, totalOverlapMinutes / 300.0);
    }
}
