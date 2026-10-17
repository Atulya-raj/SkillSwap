package com.skillswap.web.servlet;

import com.skillswap.io.CsvImporter;
import com.skillswap.model.Category;
import com.skillswap.model.Report;
import com.skillswap.model.SessionUser;
import com.skillswap.model.Skill;
import com.skillswap.model.User;
import com.skillswap.model.enums.ReportStatus;
import com.skillswap.model.enums.Role;
import com.skillswap.service.AdminService;
import com.skillswap.service.AnalyticsService;
import com.skillswap.service.CatalogService;
import com.skillswap.util.Flash;
import com.skillswap.web.filter.AuthFilter;
import com.skillswap.web.listener.ActiveUsersListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.Part;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * Administrative controller handling dashboard analytics, user moderation, credit adjustments, catalog management, and moderation reports.
 */
@WebServlet(name = "AdminServlet", urlPatterns = {"/admin", "/admin/*"})
@MultipartConfig(
        fileSizeThreshold = 1024 * 1024,      // 1 MB
        maxFileSize = 5 * 1024 * 1024,         // 5 MB
        maxRequestSize = 6 * 1024 * 1024       // 6 MB
)
public class AdminServlet extends HttpServlet {

    private final AdminService adminService;
    private final AnalyticsService analyticsService;
    private final CatalogService catalogService;

    public AdminServlet() {
        this(new AdminService(), new AnalyticsService(), new CatalogService());
    }

    public AdminServlet(AdminService adminService, AnalyticsService analyticsService, CatalogService catalogService) {
        this.adminService = adminService;
        this.analyticsService = analyticsService;
        this.catalogService = catalogService;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        SessionUser user = session != null ? (SessionUser) session.getAttribute(AuthFilter.SESSION_USER_ATTR) : null;

        if (user == null || user.getRole() != Role.ADMIN) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Administrative privileges required.");
            return;
        }

        String path = getNormalizedPath(req);

        if (path.equals("/admin") || path.equals("/admin/dashboard")) {
            handleDashboard(req, resp);
        } else if (path.equals("/admin/users")) {
            handleUsers(req, resp);
        } else if (path.equals("/admin/catalog")) {
            handleCatalog(req, resp);
        } else if (path.equals("/admin/reports")) {
            handleReports(req, resp);
        } else {
            resp.sendRedirect(req.getContextPath() + "/admin");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        SessionUser user = session != null ? (SessionUser) session.getAttribute(AuthFilter.SESSION_USER_ATTR) : null;

        if (user == null || user.getRole() != Role.ADMIN) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Administrative privileges required.");
            return;
        }

        String path = getNormalizedPath(req);

        try {
            switch (path) {
                case "/admin/users/toggle" -> handleUserToggle(req, resp);
                case "/admin/users/credits" -> handleCreditAdjustment(req, resp, user.getId());
                case "/admin/catalog/category/add" -> handleCategoryAdd(req, resp);
                case "/admin/catalog/category/delete" -> handleCategoryDelete(req, resp);
                case "/admin/catalog/skill/add" -> handleSkillAdd(req, resp);
                case "/admin/catalog/skill/delete" -> handleSkillDelete(req, resp);
                case "/admin/catalog/import" -> handleCatalogImport(req, resp);
                case "/admin/reports/resolve" -> handleReportResolve(req, resp, user.getId());
                case "/admin/reports/hide-review" -> handleHideReview(req, resp, user.getId());
                case "/admin/reports/unhide-review" -> handleUnhideReview(req, resp, user.getId());
                default -> resp.sendRedirect(req.getContextPath() + "/admin");
            }
        } catch (Exception ex) {
            Flash.error(session, "Operation failed: " + ex.getMessage());
            resp.sendRedirect(req.getContextPath() + "/admin");
        }
    }

    private String getNormalizedPath(HttpServletRequest req) {
        String servletPath = req.getServletPath() != null ? req.getServletPath() : "";
        String pathInfo = req.getPathInfo() != null ? req.getPathInfo() : "";
        String full = servletPath + pathInfo;
        if (full.endsWith("/") && full.length() > 1) {
            full = full.substring(0, full.length() - 1);
        }
        return full;
    }

    private void handleDashboard(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        int onlineCount = ActiveUsersListener.getActiveCount(getServletContext());
        AnalyticsService.AdminAnalyticsData analytics = analyticsService.adminOverview(onlineCount);
        req.setAttribute("analytics", analytics);
        req.getRequestDispatcher("/WEB-INF/jsp/admin/dashboard.jsp").forward(req, resp);
    }

    private void handleUsers(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String query = req.getParameter("q");
        int page = 1;
        try {
            String pageStr = req.getParameter("page");
            if (pageStr != null && !pageStr.isBlank()) {
                page = Math.max(1, Integer.parseInt(pageStr.trim()));
            }
        } catch (NumberFormatException ignored) {}

        int limit = 15;
        int offset = (page - 1) * limit;

        List<User> users = adminService.listUsers(query, limit, offset);
        int totalCount = adminService.countUsers(query);
        int totalPages = Math.max(1, (int) Math.ceil((double) totalCount / limit));

        req.setAttribute("users", users);
        req.setAttribute("query", query != null ? query.trim() : "");
        req.setAttribute("currentPage", page);
        req.setAttribute("totalPages", totalPages);
        req.setAttribute("totalCount", totalCount);
        req.getRequestDispatcher("/WEB-INF/jsp/admin/users.jsp").forward(req, resp);
    }

    private void handleCatalog(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        List<Category> categories = catalogService.categories();
        Map<String, List<Skill>> skillsByCategory = catalogService.skillsByCategory();
        req.setAttribute("categories", categories);
        req.setAttribute("skillsByCategory", skillsByCategory);

        // Flash warnings from recent CSV import if present
        HttpSession session = req.getSession(false);
        if (session != null) {
            Object warningsObj = session.getAttribute("importWarnings");
            if (warningsObj != null) {
                req.setAttribute("importWarnings", warningsObj);
                session.removeAttribute("importWarnings");
            }
        }

        req.getRequestDispatcher("/WEB-INF/jsp/admin/catalog.jsp").forward(req, resp);
    }

    private void handleReports(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String statusStr = req.getParameter("status");
        ReportStatus status = ReportStatus.OPEN;
        if (statusStr != null && !statusStr.isBlank()) {
            try {
                status = ReportStatus.valueOf(statusStr.trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {}
        }

        List<Report> reports = adminService.listReports(status, 50, 0);
        req.setAttribute("reports", reports);
        req.setAttribute("currentStatus", status);
        req.getRequestDispatcher("/WEB-INF/jsp/admin/reports.jsp").forward(req, resp);
    }

    private void handleUserToggle(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        long userId = Long.parseLong(req.getParameter("userId"));
        boolean active = Boolean.parseBoolean(req.getParameter("active"));
        adminService.toggleActive(userId, active);
        Flash.success(req.getSession(), "User #" + userId + " status updated to " + (active ? "ACTIVE" : "INACTIVE") + ".");
        resp.sendRedirect(req.getContextPath() + "/admin/users");
    }

    private void handleCreditAdjustment(HttpServletRequest req, HttpServletResponse resp, long adminId) throws Exception {
        long userId = Long.parseLong(req.getParameter("userId"));
        int amount = Integer.parseInt(req.getParameter("amount"));
        String reason = req.getParameter("reason");

        if (reason == null || reason.trim().isEmpty()) {
            Flash.error(req.getSession(), "A clear explanatory reason is required for manual credit adjustments.");
            resp.sendRedirect(req.getContextPath() + "/admin/users");
            return;
        }

        adminService.adjustCredits(adminId, userId, amount, reason.trim());
        Flash.success(req.getSession(), "Successfully adjusted " + (amount >= 0 ? "+" : "") + amount + " credits for User #" + userId + ".");
        resp.sendRedirect(req.getContextPath() + "/admin/users");
    }

    private void handleCategoryAdd(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        String name = req.getParameter("name");
        String desc = req.getParameter("description");
        catalogService.createCategory(name, desc);
        Flash.success(req.getSession(), "Category '" + name.trim() + "' added successfully.");
        resp.sendRedirect(req.getContextPath() + "/admin/catalog");
    }

    private void handleCategoryDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        int id = Integer.parseInt(req.getParameter("id"));
        catalogService.deleteCategory(id);
        Flash.success(req.getSession(), "Category deleted successfully.");
        resp.sendRedirect(req.getContextPath() + "/admin/catalog");
    }

    private void handleSkillAdd(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        int categoryId = Integer.parseInt(req.getParameter("categoryId"));
        String name = req.getParameter("name");
        String desc = req.getParameter("description");
        catalogService.createSkill(categoryId, name, desc);
        Flash.success(req.getSession(), "Skill '" + name.trim() + "' added successfully.");
        resp.sendRedirect(req.getContextPath() + "/admin/catalog");
    }

    private void handleSkillDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        int id = Integer.parseInt(req.getParameter("id"));
        catalogService.deleteSkill(id);
        Flash.success(req.getSession(), "Skill deleted successfully.");
        resp.sendRedirect(req.getContextPath() + "/admin/catalog");
    }

    private void handleCatalogImport(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        Part filePart = req.getPart("file");
        if (filePart == null || filePart.getSize() == 0) {
            Flash.error(req.getSession(), "Please select a valid CSV file to upload.");
            resp.sendRedirect(req.getContextPath() + "/admin/catalog");
            return;
        }

        try (InputStream is = filePart.getInputStream()) {
            CsvImporter.ImportResult result = catalogService.importCatalogCsv(is);
            if (result.hasWarnings()) {
                req.getSession().setAttribute("importWarnings", result.getWarnings());
            }
            Flash.success(req.getSession(), "CSV Import finished: " + result.getImportedCount() + " skills imported, "
                    + result.getSkippedCount() + " skipped.");
        }
        resp.sendRedirect(req.getContextPath() + "/admin/catalog");
    }

    private void handleReportResolve(HttpServletRequest req, HttpServletResponse resp, long adminId) throws IOException {
        long reportId = Long.parseLong(req.getParameter("reportId"));
        String action = req.getParameter("action");
        ReportStatus resolution = "DISMISS".equalsIgnoreCase(action) ? ReportStatus.DISMISSED : ReportStatus.RESOLVED;

        adminService.resolveReport(reportId, adminId, resolution);
        Flash.success(req.getSession(), "Report #" + reportId + " has been marked as " + resolution + ".");
        resp.sendRedirect(req.getContextPath() + "/admin/reports");
    }

    private void handleHideReview(HttpServletRequest req, HttpServletResponse resp, long adminId) throws Exception {
        long reviewId = Long.parseLong(req.getParameter("reviewId"));
        adminService.hideReview(adminId, reviewId);
        Flash.success(req.getSession(), "Review #" + reviewId + " has been hidden and author's rating stats updated.");
        resp.sendRedirect(req.getContextPath() + "/admin/reports");
    }

    private void handleUnhideReview(HttpServletRequest req, HttpServletResponse resp, long adminId) throws Exception {
        long reviewId = Long.parseLong(req.getParameter("reviewId"));
        adminService.unhideReview(adminId, reviewId);
        Flash.success(req.getSession(), "Review #" + reviewId + " has been restored.");
        resp.sendRedirect(req.getContextPath() + "/admin/reports");
    }
}
