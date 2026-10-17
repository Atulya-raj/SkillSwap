package com.skillswap.web.servlet;

import com.skillswap.model.Notification;
import com.skillswap.model.SessionUser;
import com.skillswap.service.NotificationService;
import com.skillswap.util.Flash;
import com.skillswap.util.Paginator;
import com.skillswap.web.filter.AuthFilter;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.List;

/**
 * Servlet handling in-app notifications listing, individual read marks, and bulk read actions.
 */
@WebServlet(urlPatterns = {"/notifications", "/notifications/*"})
public class NotificationServlet extends HttpServlet {

    private final NotificationService notificationService;

    /**
     * Default constructor wiring singleton NotificationService.
     */
    public NotificationServlet() {
        this(NotificationService.getInstance());
    }

    /**
     * Constructs NotificationServlet with injected service.
     *
     * @param notificationService notification service
     */
    public NotificationServlet(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        SessionUser user = session != null ? (SessionUser) session.getAttribute(AuthFilter.SESSION_USER_ATTR) : null;
        if (user == null) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }

        int page = Paginator.parsePage(req.getParameter("page"));
        int pageSize = 15;
        int total = notificationService.count(user.getId());
        Paginator paginator = new Paginator(total, pageSize, page);

        List<Notification> list = notificationService.list(user.getId(), paginator.getLimit(), paginator.getOffset());
        int unread = notificationService.unreadCount(user.getId());

        req.setAttribute("notifications", list);
        req.setAttribute("paginator", paginator);
        req.setAttribute("unreadCount", unread);

        req.getRequestDispatcher("/WEB-INF/jsp/notifications.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        SessionUser user = session != null ? (SessionUser) session.getAttribute(AuthFilter.SESSION_USER_ATTR) : null;
        if (user == null) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }

        String path = req.getPathInfo();
        String action = req.getParameter("action");

        if ("/read-all".equals(path) || "read-all".equalsIgnoreCase(action)) {
            notificationService.markAllRead(user.getId());
            Flash.success(req, "All notifications marked as read.");
            resp.sendRedirect(req.getContextPath() + "/notifications");
            return;
        }

        if ("/read".equals(path) || "read".equalsIgnoreCase(action)) {
            String idStr = req.getParameter("id");
            if (idStr != null && !idStr.trim().isEmpty()) {
                try {
                    long id = Long.parseLong(idStr.trim());
                    notificationService.markRead(id, user.getId());
                } catch (NumberFormatException ignored) {
                }
            }

            String target = req.getParameter("target");
            if (target != null && target.startsWith("/")) {
                resp.sendRedirect(req.getContextPath() + target);
            } else {
                resp.sendRedirect(req.getContextPath() + "/notifications");
            }
            return;
        }

        resp.sendRedirect(req.getContextPath() + "/notifications");
    }
}
