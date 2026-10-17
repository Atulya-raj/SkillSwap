package com.skillswap.web.filter;

import com.skillswap.model.SessionUser;
import com.skillswap.util.Flash;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

/**
 * Filter enforcing authentication on protected pages and remembering the requested URL.
 */
@WebFilter(urlPatterns = {
        "/dashboard", "/dashboard/*",
        "/profile", "/profile/*",
        "/users/*", "/users/view",
        "/skills", "/skills/*",
        "/availability", "/availability/*",
        "/browse", "/browse/*",
        "/matches", "/matches/*",
        "/requests", "/requests/*",
        "/cancel", "/confirm", "/noshow",
        "/reviews", "/reviews/*",
        "/messages", "/messages/*",
        "/notifications", "/notifications/*",
        "/credits", "/credits/*",
        "/history", "/history/*",
        "/leaderboard", "/leaderboard/*",
        "/report", "/report/*",
        "/password", "/logout",
        "/admin", "/admin/*"
})
public class AuthFilter implements Filter {

    public static final String SESSION_USER_ATTR = "currentUser";
    public static final String REDIRECT_ATTR = "redirectAfterLogin";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (!(request instanceof HttpServletRequest) || !(response instanceof HttpServletResponse)) {
            chain.doFilter(request, response);
            return;
        }

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        HttpSession session = httpRequest.getSession(false);
        SessionUser user = session != null ? (SessionUser) session.getAttribute(SESSION_USER_ATTR) : null;

        if (user == null) {
            if ("GET".equalsIgnoreCase(httpRequest.getMethod())) {
                String target = httpRequest.getRequestURI();
                String query = httpRequest.getQueryString();
                if (query != null && !query.isEmpty()) {
                    target += "?" + query;
                }
                if (session == null) {
                    session = httpRequest.getSession(true);
                }
                session.setAttribute(REDIRECT_ATTR, target);
            }
            Flash.warning(httpRequest, "Please log in to continue.");
            httpResponse.sendRedirect(httpRequest.getContextPath() + "/login");
            return;
        }

        try {
            int unread = com.skillswap.service.NotificationService.getInstance().unreadCount(user.getId());
            httpRequest.setAttribute("unreadNotificationsCount", unread);
        } catch (Exception ignored) {
        }

        chain.doFilter(request, response);
    }
}
