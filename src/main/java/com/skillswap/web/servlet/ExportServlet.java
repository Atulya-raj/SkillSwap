package com.skillswap.web.servlet;

import com.skillswap.model.SessionUser;
import com.skillswap.model.enums.Role;
import com.skillswap.service.ExportService;
import com.skillswap.web.filter.AuthFilter;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

/**
 * Controller streaming CSV exports for personal session history and administrative user rosters.
 */
@WebServlet(name = "ExportServlet", urlPatterns = {"/history/export", "/admin/users/export"})
public class ExportServlet extends HttpServlet {

    private final ExportService exportService;

    public ExportServlet() {
        this.exportService = new ExportService();
    }

    public ExportServlet(ExportService exportService) {
        this.exportService = exportService;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        SessionUser user = session != null ? (SessionUser) session.getAttribute(AuthFilter.SESSION_USER_ATTR) : null;

        if (user == null) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }

        String servletPath = req.getServletPath();

        if ("/history/export".equals(servletPath)) {
            resp.setContentType("text/csv; charset=UTF-8");
            resp.setHeader("Content-Disposition", "attachment; filename=\"skillswap-session-history.csv\"");
            exportService.exportSessionHistory(user.getId(), resp.getOutputStream());
            return;
        }

        if ("/admin/users/export".equals(servletPath)) {
            if (user.getRole() != Role.ADMIN) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Administrative privileges required.");
                return;
            }
            resp.setContentType("text/csv; charset=UTF-8");
            resp.setHeader("Content-Disposition", "attachment; filename=\"skillswap-users-roster.csv\"");
            exportService.exportUsers(resp.getOutputStream());
            return;
        }

        resp.sendError(HttpServletResponse.SC_NOT_FOUND);
    }
}
