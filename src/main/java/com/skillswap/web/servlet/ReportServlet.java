package com.skillswap.web.servlet;

import com.skillswap.dao.ReportDao;
import com.skillswap.dao.jdbc.JdbcReportDao;
import com.skillswap.model.Report;
import com.skillswap.model.SessionUser;
import com.skillswap.model.enums.ReportStatus;
import com.skillswap.util.Flash;
import com.skillswap.web.filter.AuthFilter;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * Servlet handling moderation reports against users or reviews (F-24).
 */
@WebServlet(urlPatterns = {"/report", "/report/*"})
public class ReportServlet extends HttpServlet {

    private final ReportDao reportDao;

    /**
     * Default constructor wiring standard JDBC ReportDao.
     */
    public ReportServlet() {
        this(new JdbcReportDao());
    }

    /**
     * Constructs ReportServlet with injected ReportDao.
     *
     * @param reportDao report DAO
     */
    public ReportServlet(ReportDao reportDao) {
        this.reportDao = reportDao;
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        SessionUser user = session != null ? (SessionUser) session.getAttribute(AuthFilter.SESSION_USER_ATTR) : null;
        if (user == null) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }

        String reason = req.getParameter("reason");
        if (reason == null || reason.trim().isEmpty()) {
            Flash.error(req, "Please provide a reason for the report.");
            redirectBack(req, resp);
            return;
        }

        Long targetUserId = null;
        String userParam = req.getParameter("reportedUserId");
        if (userParam != null && !userParam.trim().isEmpty()) {
            try {
                targetUserId = Long.parseLong(userParam.trim());
                if (targetUserId == user.getId()) {
                    Flash.warning(req, "You cannot report your own profile.");
                    redirectBack(req, resp);
                    return;
                }
            } catch (NumberFormatException ignored) {
            }
        }

        Long targetReviewId = null;
        String reviewParam = req.getParameter("reportedReviewId");
        if (reviewParam != null && !reviewParam.trim().isEmpty()) {
            try {
                targetReviewId = Long.parseLong(reviewParam.trim());
            } catch (NumberFormatException ignored) {
            }
        }

        if (targetUserId == null && targetReviewId == null) {
            Flash.error(req, "Invalid report: no target user or review specified.");
            redirectBack(req, resp);
            return;
        }

        Report report = new Report();
        report.setReporterId(user.getId());
        report.setTargetUserId(targetUserId);
        report.setTargetReviewId(targetReviewId);
        report.setReason(reason.trim());
        report.setStatus(ReportStatus.OPEN);
        report.setCreatedAt(LocalDateTime.now());

        reportDao.insert(report);
        Flash.success(req, "Your report has been submitted to administrators for review.");

        redirectBack(req, resp);
    }

    private void redirectBack(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String referer = req.getHeader("Referer");
        if (referer != null && !referer.trim().isEmpty()) {
            resp.sendRedirect(referer);
        } else {
            resp.sendRedirect(req.getContextPath() + "/browse");
        }
    }
}
