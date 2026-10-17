package com.skillswap.web.servlet;

import com.skillswap.exception.ResourceNotFoundException;
import com.skillswap.exception.ValidationException;
import com.skillswap.model.SessionUser;
import com.skillswap.model.User;
import com.skillswap.service.BadgeService;
import com.skillswap.service.ProfileService;
import com.skillswap.util.Flash;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.Part;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controller managing personal user profile viewing/updating, avatar uploads, and public profiles.
 */
@WebServlet(urlPatterns = {"/profile", "/profile/avatar", "/users/view"})
@MultipartConfig(
        fileSizeThreshold = 1024 * 1024,      // 1 MB
        maxFileSize = 2 * 1024 * 1024,         // 2 MB
        maxRequestSize = 4 * 1024 * 1024      // 4 MB
)
public class ProfileServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(ProfileServlet.class.getName());

    private final ProfileService profileService;
    private final BadgeService badgeService;

    public ProfileServlet() {
        this(ProfileService.getInstance(), BadgeService.getInstance());
    }

    public ProfileServlet(ProfileService profileService, BadgeService badgeService) {
        this.profileService = profileService;
        this.badgeService = badgeService;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String servletPath = request.getServletPath();

        if ("/users/view".equals(servletPath)) {
            handlePublicProfile(request, response);
            return;
        }

        // View own profile (/profile)
        SessionUser sessionUser = getSessionUser(request);
        if (sessionUser == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        try {
            User user = profileService.get(sessionUser.getId());
            request.setAttribute("profileUser", user);
            request.setAttribute("userBadges", badgeService.findForUser(sessionUser.getId()));
            request.getRequestDispatcher("/WEB-INF/jsp/profile.jsp").forward(request, response);
        } catch (ResourceNotFoundException e) {
            Flash.error(request, "User profile not found.");
            response.sendRedirect(request.getContextPath() + "/home");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String servletPath = request.getServletPath();
        SessionUser sessionUser = getSessionUser(request);
        if (sessionUser == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        if ("/profile/avatar".equals(servletPath)) {
            handleAvatarUpload(request, response, sessionUser);
        } else {
            handleProfileUpdate(request, response, sessionUser);
        }
    }

    private void handlePublicProfile(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String idParam = request.getParameter("id");
        if (idParam == null || idParam.trim().isEmpty()) {
            Flash.error(request, "User id is required to view a public profile.");
            response.sendRedirect(request.getContextPath() + "/home");
            return;
        }

        try {
            long targetUserId = Long.parseLong(idParam.trim());
            ProfileService.PublicProfileData profileData = profileService.getPublicProfile(targetUserId);
            request.setAttribute("profileData", profileData);
            request.getRequestDispatcher("/WEB-INF/jsp/public-profile.jsp").forward(request, response);
        } catch (NumberFormatException | ResourceNotFoundException e) {
            Flash.error(request, "The requested user profile does not exist.");
            response.sendRedirect(request.getContextPath() + "/home");
        }
    }

    private void handleProfileUpdate(HttpServletRequest request, HttpServletResponse response, SessionUser sessionUser)
            throws IOException {
        String fullName = request.getParameter("fullName");
        String department = request.getParameter("department");
        String studyYearStr = request.getParameter("studyYear");
        String bio = request.getParameter("bio");

        Integer studyYear = null;
        if (studyYearStr != null && !studyYearStr.trim().isEmpty()) {
            try {
                studyYear = Integer.parseInt(studyYearStr.trim());
            } catch (NumberFormatException ignored) {
            }
        }

        try {
            User updated = profileService.update(sessionUser.getId(), fullName, department, studyYear, bio);
            sessionUser.setName(updated.getFullName());
            Flash.success(request, "Profile updated successfully!");
        } catch (ValidationException e) {
            Flash.error(request, e.getUserMessage());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Profile update failed for user " + sessionUser.getId(), e);
            Flash.error(request, "Failed to update profile. Please try again.");
        }

        response.sendRedirect(request.getContextPath() + "/profile");
    }

    private void handleAvatarUpload(HttpServletRequest request, HttpServletResponse response, SessionUser sessionUser)
            throws IOException {
        try {
            Part part = request.getPart("avatar");
            String newAvatarFilename = profileService.saveAvatar(sessionUser.getId(), part);
            sessionUser.setAvatarPath(newAvatarFilename);
            Flash.success(request, "Avatar updated successfully!");
        } catch (ValidationException e) {
            Flash.error(request, e.getUserMessage());
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Avatar upload error for user " + sessionUser.getId(), e);
            Flash.error(request, "Upload failed: " + e.getMessage());
        }

        response.sendRedirect(request.getContextPath() + "/profile");
    }

    private SessionUser getSessionUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }
        return (SessionUser) session.getAttribute(AuthServlet.SESSION_USER_KEY);
    }
}
