package com.skillswap.model;

import com.skillswap.model.enums.Role;

import java.io.Serializable;

/**
 * Lightweight user session principal stored in HttpSession.
 */
public class SessionUser implements Serializable {

    private static final long serialVersionUID = 1L;

    private long id;
    private String name;
    private Role role;
    private String avatarPath;

    /**
     * Default constructor.
     */
    public SessionUser() {
    }

    /**
     * Constructs a session user principal.
     *
     * @param id user ID
     * @param name user display name
     * @param role user role
     * @param avatarPath avatar image file path
     */
    public SessionUser(long id, String name, Role role, String avatarPath) {
        this.id = id;
        this.name = name;
        this.role = role;
        this.avatarPath = avatarPath;
    }

    /**
     * Factory method creating a SessionUser from a User entity.
     *
     * @param user source User entity
     * @return new SessionUser principal
     */
    public static SessionUser fromUser(User user) {
        if (user == null) return null;
        return new SessionUser(user.getId(), user.displayName(), user.getRole(), user.getAvatarPath());
    }

    /**
     * Checks if this session user has the ADMIN role.
     *
     * @return true if admin, false otherwise
     */
    public boolean isAdmin() {
        return Role.ADMIN.equals(role);
    }

    /**
     * Checks if this session user has the STUDENT role.
     *
     * @return true if student, false otherwise
     */
    public boolean isStudent() {
        return Role.STUDENT.equals(role);
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public String getAvatarPath() {
        return avatarPath;
    }

    public void setAvatarPath(String avatarPath) {
        this.avatarPath = avatarPath;
    }

    @Override
    public String toString() {
        return "SessionUser{id=" + id + ", name='" + name + "', role=" + role + "}";
    }
}
