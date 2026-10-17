package com.skillswap.web.servlet;

import com.skillswap.model.SessionUser;
import com.skillswap.model.enums.Role;
import com.skillswap.service.AnalyticsService;
import com.skillswap.util.Flash;
import com.skillswap.web.filter.AuthFilter;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

/**
 * Controller serving the student user dashboard with aggregated metrics, active sessions, and recommendations.
 */
@WebServlet(name = "DashboardServlet", urlPatterns = {"/dashboard"})
public class DashboardServlet extends HttpServlet {

    private final AnalyticsService analyticsService;

    public DashboardServlet() {
        this.analyticsService = new AnalyticsService();
    }

    public DashboardServlet(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        SessionUser sessionUser = session != null ? (SessionUser) session.getAttribute(AuthFilter.SESSION_USER_ATTR) : null;

        if (sessionUser == null) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }

        // If an admin accesses /dashboard, redirect to /admin
        if (sessionUser.getRole() == Role.ADMIN) {
            resp.sendRedirect(req.getContextPath() + "/admin");
            return;
        }

        try {
            AnalyticsService.UserDashboardData dashboardData = analyticsService.userDashboard(sessionUser.getId());
            req.setAttribute("dashboard", dashboardData);
            req.getRequestDispatcher("/WEB-INF/jsp/dashboard.jsp").forward(req, resp);
        } catch (Exception ex) {
            Flash.error(session, "Failed to load dashboard: " + ex.getMessage());
            resp.sendRedirect(req.getContextPath() + "/");
        }
    }
}
