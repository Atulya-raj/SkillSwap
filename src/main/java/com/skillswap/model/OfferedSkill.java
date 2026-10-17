package com.skillswap.model;

import com.skillswap.exception.ValidationException;
import com.skillswap.model.enums.ListingType;

import java.util.HashMap;
import java.util.Map;

/**
 * Skill listing offered by a user to teach other students.
 */
public class OfferedSkill extends SkillListing {

    private int creditsPerHour = 1;

    /**
     * Default constructor.
     */
    public OfferedSkill() {
    }

    @Override
    public ListingType type() {
        return ListingType.OFFERED;
    }

    @Override
    public String describe() {
        String name = getSkillName() != null ? getSkillName() : "Skill";
        String lvl = getLevel() != null ? getLevel().name() : "";
        return "Teaches " + name + " (" + lvl + ") at " + creditsPerHour + " credits/hour";
    }

    @Override
    public void validate() throws ValidationException {
        Map<String, String> errors = new HashMap<>();
        if (getSkillId() <= 0) {
            errors.put("skillId", "Please select a valid skill");
        }
        if (getLevel() == null) {
            errors.put("level", "Please select a skill level");
        }
        if (creditsPerHour < 1 || creditsPerHour > 5) {
            errors.put("creditsPerHour", "Credits per hour must be between 1 and 5");
        }
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
    }

    public int getCreditsPerHour() {
        return creditsPerHour;
    }

    public void setCreditsPerHour(int creditsPerHour) {
        this.creditsPerHour = creditsPerHour;
    }

    public int getHourlyRate() {
        return creditsPerHour;
    }

    public void setHourlyRate(int hourlyRate) {
        this.creditsPerHour = hourlyRate;
    }
}
