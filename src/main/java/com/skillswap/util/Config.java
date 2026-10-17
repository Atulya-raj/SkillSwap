package com.skillswap.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Application configuration loader and accessor.
 */
public final class Config {

    private static final Logger LOGGER = Logger.getLogger(Config.class.getName());
    private static final Properties PROPERTIES = new Properties();

    static {
        loadDefault();
    }

    private Config() {
    }

    /**
     * Loads the default configuration from classpath.
     */
    public static synchronized void loadDefault() {
        try (InputStream in = Config.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (in != null) {
                PROPERTIES.clear();
                PROPERTIES.load(in);
                resolvePlaceholders(PROPERTIES);
            } else {
                LOGGER.log(Level.WARNING, "config.properties not found on classpath");
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to load config.properties", e);
        }
    }

    /**
     * Loads configuration from an input stream.
     *
     * @param in the input stream containing properties
     * @throws IOException if loading fails
     */
    public static synchronized void load(InputStream in) throws IOException {
        PROPERTIES.clear();
        PROPERTIES.load(in);
        resolvePlaceholders(PROPERTIES);
    }

    /**
     * Resolves ${user.home} and other system property placeholders.
     */
    private static void resolvePlaceholders(Properties props) {
        String userHome = System.getProperty("user.home", "");
        for (String key : props.stringPropertyNames()) {
            String val = props.getProperty(key);
            if (val != null && val.contains("${user.home}")) {
                val = val.replace("${user.home}", userHome);
                props.setProperty(key, val);
            }
        }
    }

    /**
     * Gets a string configuration property.
     *
     * @param key the property key
     * @return the property value, or null if missing
     */
    public static String getString(String key) {
        return PROPERTIES.getProperty(key);
    }

    public static String get(String key) {
        return getString(key);
    }

    /**
     * Gets a string configuration property with fallback default.
     *
     * @param key the property key
     * @param defaultValue the fallback value
     * @return the property value or default
     */
    public static String getString(String key, String defaultValue) {
        return PROPERTIES.getProperty(key, defaultValue);
    }

    public static String get(String key, String defaultValue) {
        return getString(key, defaultValue);
    }

    /**
     * Gets an integer configuration property.
     *
     * @param key the property key
     * @return the parsed integer value, or 0 if missing/invalid
     */
    public static int getInt(String key) {
        return getInt(key, 0);
    }

    /**
     * Gets an integer configuration property with fallback default.
     *
     * @param key the property key
     * @param defaultValue the fallback value
     * @return the parsed integer value or default
     */
    public static int getInt(String key, int defaultValue) {
        String val = getString(key);
        if (val == null || val.trim().isEmpty()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(val.trim());
        } catch (NumberFormatException e) {
            LOGGER.log(Level.WARNING, "Invalid integer for property {0}: {1}", new Object[]{key, val});
            return defaultValue;
        }
    }

    /**
     * Gets a long configuration property with fallback default.
     *
     * @param key the property key
     * @param defaultValue the fallback value
     * @return the parsed long value or default
     */
    public static long getLong(String key, long defaultValue) {
        String val = getString(key);
        if (val == null || val.trim().isEmpty()) {
            return defaultValue;
        }
        try {
            return Long.parseLong(val.trim());
        } catch (NumberFormatException e) {
            LOGGER.log(Level.WARNING, "Invalid long for property {0}: {1}", new Object[]{key, val});
            return defaultValue;
        }
    }

    /**
     * Gets a boolean configuration property.
     *
     * @param key the property key
     * @return true if property is set to 'true' (case-insensitive), false otherwise
     */
    public static boolean getBoolean(String key) {
        return getBoolean(key, false);
    }

    /**
     * Gets a boolean configuration property with fallback default.
     *
     * @param key the property key
     * @param defaultValue the fallback value
     * @return the boolean value or default
     */
    public static boolean getBoolean(String key, boolean defaultValue) {
        String val = getString(key);
        if (val == null || val.trim().isEmpty()) {
            return defaultValue;
        }
        return Boolean.parseBoolean(val.trim());
    }

    /**
     * Sets a property in memory (useful for testing).
     *
     * @param key the property key
     * @param value the property value
     */
    public static synchronized void setProperty(String key, String value) {
        PROPERTIES.setProperty(key, value);
    }
}
