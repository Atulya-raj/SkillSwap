package com.skillswap.badge;

import com.skillswap.model.User;

/**
 * Awards CONNECTOR badge when user offers 3 or more skills.
 */
public class ConnectorRule implements BadgeRule {

    @Override
    public String code() {
        return "CONNECTOR";
    }

    @Override
    public boolean qualifies(User user, BadgeStats stats) {
        return stats.getOfferedSkillsCount() >= 3;
    }
}
