package com.skillswap.dao;

import com.skillswap.model.enums.SessionStatus;

import java.util.List;
import java.util.Map;

/**
 * Data access interface for administrative metrics and reporting.
 */
public interface AnalyticsDao {

    int totalUsers();

    Map<SessionStatus, Integer> sessionsByStatus();

    List<SkillCount> topWanted(int n);

    List<SkillCount> topOffered(int n);

    List<WeekCount> newUsersPerWeek(int weeks);

    /**
     * DTO for skill demand and offer counts.
     */
    class SkillCount {
        private final String skillName;
        private final int count;

        public SkillCount(String skillName, int count) {
            this.skillName = skillName;
            this.count = count;
        }

        public String getSkillName() {
            return skillName;
        }

        public int getCount() {
            return count;
        }
    }

    /**
     * DTO for new user registration trends by week.
     */
    class WeekCount {
        private final String weekLabel;
        private final int count;

        public WeekCount(String weekLabel, int count) {
            this.weekLabel = weekLabel;
            this.count = count;
        }

        public String getWeekLabel() {
            return weekLabel;
        }

        public int getCount() {
            return count;
        }
    }
}
