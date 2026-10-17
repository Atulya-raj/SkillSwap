package com.skillswap.model;

import com.skillswap.model.enums.Role;

import java.util.ArrayList;
import java.util.List;

/**
 * Domain model representing a student user who can teach and learn skills.
 */
public class Student extends User {

    private List<OfferedSkill> offered = new ArrayList<>();
    private List<WantedSkill> wanted = new ArrayList<>();

    /**
     * Constructs a student user.
     */
    public Student() {
        setRole(Role.STUDENT);
    }

    @Override
    public String homePath() {
        return "/dashboard";
    }

    public List<OfferedSkill> getOffered() {
        return offered;
    }

    public void setOffered(List<OfferedSkill> offered) {
        this.offered = offered != null ? offered : new ArrayList<>();
    }

    public List<WantedSkill> getWanted() {
        return wanted;
    }

    public void setWanted(List<WantedSkill> wanted) {
        this.wanted = wanted != null ? wanted : new ArrayList<>();
    }
}
