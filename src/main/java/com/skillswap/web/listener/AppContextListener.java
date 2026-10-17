package com.skillswap.web.listener;

import com.skillswap.db.Database;
import com.skillswap.db.DatabaseInitializer;
import com.skillswap.util.Config;
import com.skillswap.dao.UserDao;
import com.skillswap.dao.jdbc.JdbcUserDao;
import com.skillswap.model.Admin;
import com.skillswap.model.enums.Role;
import com.skillswap.util.PasswordHasher;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Application context lifecycle listener managing database and background thread initialization.
 */
@WebListener
public class AppContextListener implements ServletContextListener {

    private static final Logger LOGGER = Logger.getLogger(AppContextListener.class.getName());

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        LOGGER.log(Level.INFO, "SkillSwap web application starting up...");
        try {
            // 1. Load application configuration
            Config.loadDefault();

            // 2. Initialize connection pool
            Database.init();

            // 3. Run schema and seed initializer if enabled
            if (Config.getBoolean("db.autoinit", true)) {
                new DatabaseInitializer().initialize();
            }

            // 4. Seed admin account if none exists
            seedAdminAccountIfMissing();

            // 5. Initialize skill catalog cache
            com.skillswap.service.CatalogService.getInstance().refreshCache();

            // 6. Start notification dispatcher daemon and background scheduler
            com.skillswap.service.NotificationDispatcher.getInstance().start();
            com.skillswap.scheduler.SchedulerService.getInstance().start();

            LOGGER.log(Level.INFO, "SkillSwap application initialized successfully.");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to initialize SkillSwap application context", e);
            throw new RuntimeException("Application startup failure", e);
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        LOGGER.log(Level.INFO, "SkillSwap web application shutting down...");
        try {
            try {
                com.skillswap.scheduler.SchedulerService.getInstance().stop();
            } catch (Throwable t) {
                LOGGER.log(Level.WARNING, "Error stopping SchedulerService", t);
            }
            try {
                com.skillswap.service.NotificationDispatcher.getInstance().stop();
            } catch (Throwable t) {
                LOGGER.log(Level.WARNING, "Error stopping NotificationDispatcher", t);
            }
            Database.getInstance().shutdown();
            LOGGER.log(Level.INFO, "Database connection pool shut down cleanly.");
            try {
                com.mysql.cj.jdbc.AbandonedConnectionCleanupThread.checkedShutdown();
            } catch (Throwable ignored) {
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error during shutdown", e);
        }
    }

    private void seedAdminAccountIfMissing() {
        try {
            UserDao userDao = new JdbcUserDao();
            String adminEmail = Config.get("admin.email", "admin@skillswap.local");
            if (!userDao.emailExists(adminEmail)) {
                String adminPassword = Config.get("admin.password", "ChangeMe123");
                Admin admin = new Admin();
                admin.setFullName("System Administrator");
                admin.setEmail(adminEmail);
                admin.setPasswordHash(PasswordHasher.hash(adminPassword));
                admin.setRole(Role.ADMIN);
                admin.setActive(true);
                admin.setCredits(0);
                userDao.insert(admin);
                LOGGER.log(Level.INFO, "Seeded default administrator account: {0}", adminEmail);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to seed default administrator account", e);
        }
    }
}
