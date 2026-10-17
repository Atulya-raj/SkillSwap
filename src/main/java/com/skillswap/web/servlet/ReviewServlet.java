package com.skillswap.web.servlet;

import com.skillswap.exception.SkillSwapException;
import com.skillswap.model.SessionUser;
import com.skillswap.service.ReviewService;
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
 * Servlet handling submission of post-session ratings and reviews.
 */
@WebServlet(urlPatterns = {"/reviews", "/reviews/*"})
public class ReviewServlet extends HttpServlet {

    private final ReviewService reviewService;

    /**
     * Default constructor wiring ReviewService singleton.
     */
    public ReviewServlet() {
        this(ReviewService.getInstance());
    }

    /**
     * Constructs ReviewServlet with injected ReviewService.
     *
     * @param reviewService review service
     */
    public ReviewServlet(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        SessionUser user = session != null ? (SessionUser) session.getAttribute(AuthFilter.SESSION_USER_ATTR) : null;
        if (user == null) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }

        String sessionIdStr = req.getParameter("sessionId");
        long sessionId = 0;
        try {
            if (sessionIdStr != null) {
                sessionId = Long.parseLong(sessionIdStr.trim());
            }
        } catch (NumberFormatException e) {
            Flash.error(req, "Invalid session request ID.");
            resp.sendRedirect(req.getContextPath() + "/requests");
            return;
        }

        String ratingStr = req.getParameter("rating");
        int rating = 0;
        try {
            if (ratingStr != null) {
                rating = Integer.parseInt(ratingStr.trim());
            }
        } catch (NumberFormatException ignored) {
        }

        String comment = req.getParameter("comment");

        try {
            reviewService.submit(user.getId(), sessionId, rating, comment);
            Flash.success(req, "Review submitted successfully! Thank you for rating your peer.");
        } catch (SkillSwapException e) {
            Flash.error(req, e.getMessage());
        } catch (Exception e) {
            Flash.error(req, "An error occurred while submitting your review: " + e.getMessage());
        }

        resp.sendRedirect(req.getContextPath() + "/requests/view?id=" + sessionId);
    }
}
