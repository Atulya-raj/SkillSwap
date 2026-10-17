package com.skillswap.model;

import com.skillswap.exception.ValidationException;
import com.skillswap.model.enums.ListingType;

import java.util.HashMap;
import java.util.Map;

/**
 * Skill listing wanted by a user to learn from other students.
 */
public class WantedSkill extends SkillListing {

    /**
     * Default constructor.
     */
    public WantedSkill() {
    }

    @Override
    public ListingType type() {
        return ListingType.WANTED;
    }

    @Override
    public String describe() {
        String name = getSkillName() != null ? getSkillName() : "Skill";
        String lvl = getLevel() != null ? getLevel().name() : "";
        return "Wants to learn " + name + " (" + lvl + ")";
    }

    @Override
    public void validate() throws ValidationException {
        Map<String, String> errors = new HashMap<>();
        if (getSkillId() <= 0) {
            errors.put("skillId", "Please select a valid skill");
        }
        if (getLevel() == null) {
            errors.put("level", "Please select a target level");
        }
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
    }
}
