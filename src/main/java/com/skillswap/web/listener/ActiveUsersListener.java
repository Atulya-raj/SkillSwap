package com.skillswap.web.listener;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Listener tracking the number of active HTTP sessions across the application.
 */
@WebListener
public class ActiveUsersListener implements HttpSessionListener, ServletContextListener {

    public static final String ACTIVE_USERS_ATTR = "activeUsersCount";
    private final AtomicInteger activeUsers = new AtomicInteger(0);

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        sce.getServletContext().setAttribute(ACTIVE_USERS_ATTR, activeUsers);
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        sce.getServletContext().removeAttribute(ACTIVE_USERS_ATTR);
    }

    @Override
    public void sessionCreated(HttpSessionEvent se) {
        ServletContext ctx = se.getSession().getServletContext();
        AtomicInteger counter = (AtomicInteger) ctx.getAttribute(ACTIVE_USERS_ATTR);
        if (counter != null) {
            counter.incrementAndGet();
        } else {
            activeUsers.incrementAndGet();
        }
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent se) {
        ServletContext ctx = se.getSession().getServletContext();
        AtomicInteger counter = (AtomicInteger) ctx.getAttribute(ACTIVE_USERS_ATTR);
        if (counter != null) {
            counter.decrementAndGet();
        } else {
            activeUsers.decrementAndGet();
        }
    }

    /**
     * Retrieves the current number of active user sessions.
     *
     * @param ctx servlet context
     * @return active session count, or 0 if unavailable
     */
    public static int getActiveCount(ServletContext ctx) {
        if (ctx == null) {
            return 0;
        }
        Object attr = ctx.getAttribute(ACTIVE_USERS_ATTR);
        if (attr instanceof AtomicInteger) {
            return Math.max(0, ((AtomicInteger) attr).get());
        }
        return 0;
    }
}
