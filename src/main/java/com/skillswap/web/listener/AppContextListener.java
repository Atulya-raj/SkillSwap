package com.skillswap.web.listener;

import com.skillswap.db.Database;
import com.skillswap.db.DatabaseInitializer;
import com.skillswap.util.Config;
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
            Database.getInstance().shutdown();
            LOGGER.log(Level.INFO, "Database connection pool shut down cleanly.");
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error during shutdown", e);
        }
    }
}
