package com.skillswap.util;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Thread-safe date and time formatting utilities tailored for Asia/Kolkata timezone.
 */
public final class DateUtil {

    public static final ZoneId ZONE_KOLKATA = ZoneId.of("Asia/Kolkata");

    private static final DateTimeFormatter ISO_DT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
    private static final DateTimeFormatter FRIENDLY_DT = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a", Locale.ENGLISH);
    private static final DateTimeFormatter DATE_ONLY = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter TIME_ONLY = DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH);

    private DateUtil() {
    }

    /**
     * Returns the current date and time in Asia/Kolkata timezone.
     *
     * @return current LocalDateTime
     */
    public static LocalDateTime now() {
        return LocalDateTime.now(ZONE_KOLKATA);
    }

    /**
     * Formats a LocalDateTime into a friendly format e.g. "18 Oct 2026, 02:30 PM".
     *
     * @param dt the LocalDateTime
     * @return formatted string, or empty string if null
     */
    public static String formatFriendly(LocalDateTime dt) {
        if (dt == null) return "";
        return dt.format(FRIENDLY_DT);
    }

    /**
     * Formats a LocalDateTime into date-only format e.g. "18 Oct 2026".
     *
     * @param dt the LocalDateTime
     * @return formatted date string, or empty string if null
     */
    public static String formatDate(LocalDateTime dt) {
        if (dt == null) return "";
        return dt.format(DATE_ONLY);
    }

    /**
     * Formats a LocalTime into 12-hour format e.g. "02:30 PM".
     *
     * @param time the LocalTime
     * @return formatted time string, or empty string if null
     */
    public static String formatTime(LocalTime time) {
        if (time == null) return "";
        return time.format(TIME_ONLY);
    }

    /**
     * Formats a LocalDateTime into standard HTML5 datetime-local input format "yyyy-MM-dd'T'HH:mm".
     *
     * @param dt the LocalDateTime
     * @return ISO datetime string, or empty string if null
     */
    public static String formatIso(LocalDateTime dt) {
        if (dt == null) return "";
        return dt.format(ISO_DT);
    }

    /**
     * Parses an ISO datetime string e.g. "2026-10-18T14:30".
     *
     * @param text input string
     * @return parsed LocalDateTime, or null if input is null/empty
     */
    public static LocalDateTime parseIso(String text) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        return LocalDateTime.parse(text.trim());
    }
}
