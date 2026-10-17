package com.skillswap.util;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import java.io.Serializable;

/**
 * Session-scoped one-shot flash message helper for Post-Redirect-Get pattern.
 */
public final class Flash {

    private static final String SESSION_KEY = "SKILLSWAP_FLASH_MESSAGE";

    private Flash() {
    }

    /**
     * Flash message payload container.
     */
    public static class FlashMessage implements Serializable {
        private static final long serialVersionUID = 1L;
        private final String type;
        private final String message;

        public FlashMessage(String type, String message) {
            this.type = type;
            this.message = message;
        }

        public String getType() {
            return type;
        }

        public String getMessage() {
            return message;
        }
    }

    /**
     * Sets a success flash message.
     *
     * @param req HTTP request
     * @param message notification message text
     */
    public static void success(HttpServletRequest req, String message) {
        set(req, "success", message);
    }

    /**
     * Sets a success flash message on session.
     *
     * @param session HTTP session
     * @param message notification message text
     */
    public static void success(HttpSession session, String message) {
        setSession(session, "success", message);
    }

    /**
     * Sets an error flash message.
     *
     * @param req HTTP request
     * @param message error message text
     */
    public static void error(HttpServletRequest req, String message) {
        set(req, "error", message);
    }

    /**
     * Sets an error flash message on session.
     *
     * @param session HTTP session
     * @param message error message text
     */
    public static void error(HttpSession session, String message) {
        setSession(session, "error", message);
    }

    /**
     * Sets a warning flash message.
     *
     * @param req HTTP request
     * @param message warning message text
     */
    public static void warning(HttpServletRequest req, String message) {
        set(req, "warning", message);
    }

    /**
     * Sets a warning flash message on session.
     *
     * @param session HTTP session
     * @param message warning message text
     */
    public static void warning(HttpSession session, String message) {
        setSession(session, "warning", message);
    }

    /**
     * Sets an informational flash message.
     *
     * @param req HTTP request
     * @param message info message text
     */
    public static void info(HttpServletRequest req, String message) {
        set(req, "info", message);
    }

    /**
     * Sets an informational flash message on session.
     *
     * @param session HTTP session
     * @param message info message text
     */
    public static void info(HttpSession session, String message) {
        setSession(session, "info", message);
    }

    private static void set(HttpServletRequest req, String type, String message) {
        setSession(req.getSession(true), type, message);
    }

    private static void setSession(HttpSession session, String type, String message) {
        if (session != null) {
            session.setAttribute(SESSION_KEY, new FlashMessage(type, message));
            // Direct session attribute for easy JSTL EL access
            String attrName = "flash" + Character.toUpperCase(type.charAt(0)) + type.substring(1);
            session.setAttribute(attrName, message);
        }
    }

    /**
     * Retrieves and removes any active flash message from the session.
     *
     * @param req HTTP request
     * @return active FlashMessage or null
     */
    public static FlashMessage consume(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        if (session == null) {
            return null;
        }
        FlashMessage msg = (FlashMessage) session.getAttribute(SESSION_KEY);
        if (msg != null) {
            session.removeAttribute(SESSION_KEY);
            String attrName = "flash" + Character.toUpperCase(msg.getType().charAt(0)) + msg.getType().substring(1);
            req.setAttribute(attrName, msg.getMessage());
            session.removeAttribute(attrName);
        }
        return msg;
    }
}
