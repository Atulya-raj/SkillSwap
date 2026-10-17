package com.skillswap.model;

import com.skillswap.model.enums.Role;

/**
 * Domain model representing a system administrator.
 */
public class Admin extends User {

    /**
     * Constructs an administrator user.
     */
    public Admin() {
        setRole(Role.ADMIN);
    }

    @Override
    public String homePath() {
        return "/admin";
    }
}
