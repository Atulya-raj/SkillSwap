package com.skillswap.model;

/**
 * Result produced by the matching engine representing a ranked provider candidate.
 */
public class MatchResult implements Comparable<MatchResult> {

    private User provider;
    private OfferedSkill listing;
    private double score;
    private double skillRelevance;
    private double ratingScore;
    private double availabilityOverlap;

    /**
     * Default constructor.
     */
    public MatchResult() {
    }

    /**
     * Constructs a MatchResult with candidate details and breakdown scores.
     *
     * @param provider the provider user
     * @param listing the offered skill listing
     * @param score total combined score
     * @param skillRelevance skill similarity score
     * @param ratingScore user rating score
     * @param availabilityOverlap schedule overlap score
     */
    public MatchResult(User provider, OfferedSkill listing, double score,
                       double skillRelevance, double ratingScore, double availabilityOverlap) {
        this.provider = provider;
        this.listing = listing;
        this.score = score;
        this.skillRelevance = skillRelevance;
        this.ratingScore = ratingScore;
        this.availabilityOverlap = availabilityOverlap;
    }

    @Override
    public int compareTo(MatchResult other) {
        // Natural ordering: highest score first
        return Double.compare(other.score, this.score);
    }

    public User getProvider() {
        return provider;
    }

    public void setProvider(User provider) {
        this.provider = provider;
    }

    public OfferedSkill getListing() {
        return listing;
    }

    public void setListing(OfferedSkill listing) {
        this.listing = listing;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    public double getSkillRelevance() {
        return skillRelevance;
    }

    public void setSkillRelevance(double skillRelevance) {
        this.skillRelevance = skillRelevance;
    }

    public double getRatingScore() {
        return ratingScore;
    }

    public void setRatingScore(double ratingScore) {
        this.ratingScore = ratingScore;
    }

    public double getAvailabilityOverlap() {
        return availabilityOverlap;
    }

    public void setAvailabilityOverlap(double availabilityOverlap) {
        this.availabilityOverlap = availabilityOverlap;
    }

    public int getScorePercent() {
        return (int) Math.min(100, Math.max(0, Math.round(score * 100)));
    }

    public int getRelevancePercent() {
        return (int) Math.min(100, Math.max(0, Math.round(skillRelevance * 100)));
    }

    public int getRatingPercent() {
        return (int) Math.min(100, Math.max(0, Math.round(ratingScore * 100)));
    }

    public int getAvailabilityPercent() {
        return (int) Math.min(100, Math.max(0, Math.round(availabilityOverlap * 100)));
    }

    @Override
    public String toString() {
        return "MatchResult{provider=" + (provider != null ? provider.displayName() : "null") + ", score=" + score + "}";
    }
}
