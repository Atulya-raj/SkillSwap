package com.skillswap.web.servlet;

import com.skillswap.exception.NoMatchFoundException;
import com.skillswap.model.MatchResult;
import com.skillswap.model.SessionUser;
import com.skillswap.model.WantedSkill;
import com.skillswap.service.ListingService;
import com.skillswap.service.MatchService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Controller providing personalized skill recommendations and match scoring breakdown.
 */
@WebServlet("/matches")
public class MatchServlet extends HttpServlet {

    private final MatchService matchService;
    private final ListingService listingService;

    public MatchServlet() {
        this(MatchService.getInstance(), ListingService.getInstance());
    }

    public MatchServlet(MatchService matchService, ListingService listingService) {
        this.matchService = matchService;
        this.listingService = listingService;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        SessionUser sessionUser = getSessionUser(request);
        if (sessionUser == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        long userId = sessionUser.getId();
        List<WantedSkill> wantedSkills = listingService.findWantedByUser(userId);

        String skillIdParam = request.getParameter("skillId");
        List<MatchResult> matches = new ArrayList<>();
        boolean noMatchFound = false;

        if (skillIdParam != null && !skillIdParam.trim().isEmpty()) {
            try {
                long skillId = Long.parseLong(skillIdParam.trim());
                matches = matchService.findFor(userId, skillId, 10);
                request.setAttribute("selectedSkillId", skillId);
            } catch (NoMatchFoundException e) {
                noMatchFound = true;
            } catch (NumberFormatException ignored) {
            }
        } else {
            matches = matchService.recommend(userId, 10);
            if (matches.isEmpty()) {
                noMatchFound = true;
            }
        }

        request.setAttribute("matches", matches);
        request.setAttribute("wantedSkills", wantedSkills);
        request.setAttribute("noMatchFound", noMatchFound);

        request.getRequestDispatcher("/WEB-INF/jsp/matches.jsp").forward(request, response);
    }

    private SessionUser getSessionUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }
        return (SessionUser) session.getAttribute(AuthServlet.SESSION_USER_KEY);
    }
}
