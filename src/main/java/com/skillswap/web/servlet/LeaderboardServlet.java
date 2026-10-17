package com.skillswap.web.servlet;

import com.skillswap.dao.BadgeDao;
import com.skillswap.dao.UserDao;
import com.skillswap.dao.jdbc.JdbcBadgeDao;
import com.skillswap.dao.jdbc.JdbcUserDao;
import com.skillswap.model.Badge;
import com.skillswap.model.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Servlet presenting the campus skill leaderboard ranking top mentors by rating and sessions taught (F-21).
 */
@WebServlet(urlPatterns = {"/leaderboard", "/leaderboard/*"})
public class LeaderboardServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(LeaderboardServlet.class.getName());

    private final UserDao userDao;
    private final BadgeDao badgeDao;

    /**
     * Default constructor wiring standard DAOs.
     */
    public LeaderboardServlet() {
        this(new JdbcUserDao(), new JdbcBadgeDao());
    }

    /**
     * Constructs LeaderboardServlet with injected DAOs.
     *
     * @param userDao user DAO
     * @param badgeDao badge DAO
     */
    public LeaderboardServlet(UserDao userDao, BadgeDao badgeDao) {
        this.userDao = userDao;
        this.badgeDao = badgeDao;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            List<User> topByRating = userDao.topMentorsByRating(3, 10);
            List<User> topBySessions = userDao.topMentorsBySessions(10);

            Map<Long, List<Badge>> badgesMap = new HashMap<>();
            for (User u : topByRating) {
                badgesMap.computeIfAbsent(u.getId(), badgeDao::findForUser);
            }
            for (User u : topBySessions) {
                badgesMap.computeIfAbsent(u.getId(), badgeDao::findForUser);
            }

            req.setAttribute("topByRating", topByRating);
            req.setAttribute("topBySessions", topBySessions);
            req.setAttribute("badgesMap", badgesMap);

            req.getRequestDispatcher("/WEB-INF/jsp/leaderboard.jsp").forward(req, resp);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed generating leaderboard", e);
            throw new ServletException(e);
        }
    }
}
