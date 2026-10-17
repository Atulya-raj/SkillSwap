package com.skillswap.web.servlet;

import com.skillswap.exception.ValidationException;
import com.skillswap.model.Category;
import com.skillswap.model.OfferedSkill;
import com.skillswap.model.SessionUser;
import com.skillswap.model.Skill;
import com.skillswap.model.WantedSkill;
import com.skillswap.model.enums.SkillLevel;
import com.skillswap.service.CatalogService;
import com.skillswap.service.ListingService;
import com.skillswap.util.Flash;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controller for managing a student's offered and wanted skills.
 */
@WebServlet("/skills")
public class ListingServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(ListingServlet.class.getName());

    private final ListingService listingService;
    private final CatalogService catalogService;

    public ListingServlet() {
        this(ListingService.getInstance(), CatalogService.getInstance());
    }

    public ListingServlet(ListingService listingService, CatalogService catalogService) {
        this.listingService = listingService;
        this.catalogService = catalogService;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        SessionUser sessionUser = getSessionUser(request);
        if (sessionUser == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        List<OfferedSkill> offered = listingService.findOfferedByUser(sessionUser.getId());
        List<WantedSkill> wanted = listingService.findWantedByUser(sessionUser.getId());
        List<Category> categories = catalogService.categories();
        Map<String, List<Skill>> skillsByCategory = catalogService.skillsByCategory();
        List<Skill> allSkills = catalogService.allSkills();

        request.setAttribute("offeredSkills", offered);
        request.setAttribute("wantedSkills", wanted);
        request.setAttribute("categories", categories);
        request.setAttribute("skillsByCategory", skillsByCategory);
        request.setAttribute("allSkills", allSkills);
        request.setAttribute("skillLevels", SkillLevel.values());

        request.getRequestDispatcher("/WEB-INF/jsp/my-skills.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        SessionUser sessionUser = getSessionUser(request);
        if (sessionUser == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        String action = request.getParameter("action");
        try {
            if ("offer".equalsIgnoreCase(action)) {
                handleOffer(request, sessionUser);
            } else if ("want".equalsIgnoreCase(action)) {
                handleWant(request, sessionUser);
            } else if ("remove".equalsIgnoreCase(action)) {
                handleRemove(request, sessionUser);
            } else {
                Flash.error(request, "Unknown action requested.");
            }
        } catch (ValidationException e) {
            Flash.error(request, e.getUserMessage());
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error handling skill listing action: " + action, e);
            Flash.error(request, "Operation failed: " + e.getMessage());
        }

        response.sendRedirect(request.getContextPath() + "/skills");
    }

    private void handleOffer(HttpServletRequest request, SessionUser sessionUser) throws ValidationException {
        int skillId = parseInteger(request.getParameter("skillId"), 0);
        SkillLevel level = parseLevel(request.getParameter("level"), SkillLevel.INTERMEDIATE);
        int creditsPerHour = parseInteger(request.getParameter("creditsPerHour"), 1);
        String description = request.getParameter("description");

        listingService.offer(sessionUser.getId(), skillId, level, creditsPerHour, description);
        Flash.success(request, "Offered skill added successfully!");
    }

    private void handleWant(HttpServletRequest request, SessionUser sessionUser) throws ValidationException {
        int skillId = parseInteger(request.getParameter("skillId"), 0);
        SkillLevel level = parseLevel(request.getParameter("level"), SkillLevel.BEGINNER);
        String description = request.getParameter("description");

        listingService.want(sessionUser.getId(), skillId, level, description);
        Flash.success(request, "Wanted skill added successfully!");
    }

    private void handleRemove(HttpServletRequest request, SessionUser sessionUser) throws Exception {
        long listingId = parseLong(request.getParameter("id"), 0L);
        if (listingId <= 0) {
            Flash.error(request, "Invalid listing identifier.");
            return;
        }
        listingService.remove(sessionUser.getId(), listingId);
        Flash.success(request, "Skill listing removed.");
    }

    private int parseInteger(String val, int defaultVal) {
        if (val == null || val.trim().isEmpty()) return defaultVal;
        try {
            return Integer.parseInt(val.trim());
        } catch (NumberFormatException e) {
            return defaultVal;
        }
    }

    private long parseLong(String val, long defaultVal) {
        if (val == null || val.trim().isEmpty()) return defaultVal;
        try {
            return Long.parseLong(val.trim());
        } catch (NumberFormatException e) {
            return defaultVal;
        }
    }

    private SkillLevel parseLevel(String val, SkillLevel defaultLevel) {
        if (val == null || val.trim().isEmpty()) return defaultLevel;
        try {
            return SkillLevel.valueOf(val.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return defaultLevel;
        }
    }

    private SessionUser getSessionUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }
        return (SessionUser) session.getAttribute(AuthServlet.SESSION_USER_KEY);
    }
}
