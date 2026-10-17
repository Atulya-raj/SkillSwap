package com.skillswap.util;

import com.skillswap.exception.ValidationException;
import com.skillswap.model.enums.SessionMode;

import java.io.InputStream;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Centralized business input validation rules.
 */
public final class Validator {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$"
    );
    private static final Set<Integer> VALID_DURATIONS = Set.of(30, 60, 90, 120);

    private Validator() {
    }

    /**
     * Checks if an email address string is syntactically valid and within length limits.
     *
     * @param email email string to validate
     * @return true if valid, false otherwise
     */
    public static boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        String trimmed = email.trim();
        if (trimmed.length() > 150) {
            return false;
        }
        return EMAIL_PATTERN.matcher(trimmed).matches();
    }

    /**
     * Checks if a password string meets minimum length and character complexity requirements.
     *
     * @param password password string to validate
     * @return true if strong, false otherwise
     */
    public static boolean isStrongPassword(String password) {
        if (password == null || password.length() < 8) {
            return false;
        }
        boolean hasLetter = false;
        boolean hasDigit = false;
        for (char c : password.toCharArray()) {
            if (Character.isLetter(c)) hasLetter = true;
            if (Character.isDigit(c)) hasDigit = true;
        }
        return hasLetter && hasDigit;
    }

    /**
     * Validates user registration fields.
     *
     * @param fullName user full name
     * @param email user email address
     * @param password plaintext password
     * @param department student department
     * @param studyYear student year of study (1-5)
     * @throws ValidationException if any field is invalid
     */
    public static void validateRegistration(String fullName, String email, String password,
                                             String department, Integer studyYear) throws ValidationException {
        Map<String, String> errors = new HashMap<>();

        if (fullName == null || fullName.trim().length() < 2 || fullName.trim().length() > 100) {
            errors.put("fullName", "Name must be between 2 and 100 characters");
        }

        validateEmailField(email, errors);
        validatePasswordField(password, errors);

        if (studyYear != null && (studyYear < 1 || studyYear > 5)) {
            errors.put("studyYear", "Study year must be between 1 and 5");
        }

        if (department != null && department.trim().length() > 100) {
            errors.put("department", "Department cannot exceed 100 characters");
        }

        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
    }

    /**
     * Validates user profile update fields.
     *
     * @param fullName user full name
     * @param department student department
     * @param studyYear student year of study (1-5)
     * @param bio user bio
     * @throws ValidationException if any field is invalid
     */
    public static void validateProfile(String fullName, String department, Integer studyYear, String bio)
            throws ValidationException {
        Map<String, String> errors = new HashMap<>();

        if (fullName == null || fullName.trim().length() < 2 || fullName.trim().length() > 100) {
            errors.put("fullName", "Name must be between 2 and 100 characters");
        }
        if (studyYear != null && (studyYear < 1 || studyYear > 5)) {
            errors.put("studyYear", "Study year must be between 1 and 5");
        }
        if (department != null && department.trim().length() > 100) {
            errors.put("department", "Department cannot exceed 100 characters");
        }
        if (bio != null && bio.length() > 500) {
            errors.put("bio", "Bio cannot exceed 500 characters");
        }

        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
    }

    private static void validateEmailField(String email, Map<String, String> errors) {
        if (email == null || email.trim().isEmpty()) {
            errors.put("email", "Email is required");
            return;
        }
        String trimmed = email.trim();
        if (trimmed.length() > 150) {
            errors.put("email", "Email cannot exceed 150 characters");
            return;
        }
        if (!EMAIL_PATTERN.matcher(trimmed).matches()) {
            errors.put("email", "Please enter a valid email address");
            return;
        }

        String allowedDomain = Config.getString("allowed.email.domain");
        if (allowedDomain != null && !allowedDomain.trim().isEmpty()) {
            String domain = allowedDomain.trim();
            if (!domain.startsWith("@")) {
                domain = "@" + domain;
            }
            if (!trimmed.toLowerCase().endsWith(domain.toLowerCase())) {
                errors.put("email", "Email must belong to domain " + domain);
            }
        }
    }

    private static void validatePasswordField(String password, Map<String, String> errors) {
        if (password == null || password.length() < 8) {
            errors.put("password", "Password must be at least 8 characters long");
            return;
        }
        boolean hasLetter = false;
        boolean hasDigit = false;
        for (char c : password.toCharArray()) {
            if (Character.isLetter(c)) hasLetter = true;
            if (Character.isDigit(c)) hasDigit = true;
        }
        if (!hasLetter || !hasDigit) {
            errors.put("password", "Password must contain at least one letter and one number");
        }
    }

    /**
     * Validates session request submission parameters.
     *
     * @param proposedStart proposed start time
     * @param durationMinutes duration in minutes
     * @param mode session mode
     * @param meetingDetails link or location details
     * @param message optional message
     * @throws ValidationException if bounds or parameters are invalid
     */
    public static void validateSessionRequest(LocalDateTime proposedStart, int durationMinutes,
                                              SessionMode mode, String meetingDetails, String message)
            throws ValidationException {
        Map<String, String> errors = new HashMap<>();
        LocalDateTime now = LocalDateTime.now();

        if (proposedStart == null) {
            errors.put("proposedStart", "Proposed start date and time is required");
        } else {
            LocalDateTime minStart = now.plusHours(2).minusMinutes(1);
            LocalDateTime maxStart = now.plusDays(30);
            if (proposedStart.isBefore(minStart)) {
                errors.put("proposedStart", "Sessions must be scheduled at least 2 hours in advance");
            } else if (proposedStart.isAfter(maxStart)) {
                errors.put("proposedStart", "Sessions cannot be scheduled more than 30 days in advance");
            }
        }

        if (!VALID_DURATIONS.contains(durationMinutes)) {
            errors.put("durationMinutes", "Duration must be 30, 60, 90, or 120 minutes");
        }

        if (mode == null) {
            errors.put("mode", "Session mode is required");
        } else {
            if (meetingDetails == null || meetingDetails.trim().isEmpty()) {
                if (mode == SessionMode.ONLINE) {
                    errors.put("meetingDetails", "Online sessions require a meeting link");
                } else {
                    errors.put("meetingDetails", "In-person sessions require a meeting place");
                }
            } else if (meetingDetails.trim().length() > 255) {
                errors.put("meetingDetails", "Meeting details cannot exceed 255 characters");
            }
        }

        if (message != null && message.length() > 500) {
            errors.put("message", "Message cannot exceed 500 characters");
        }

        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
    }

    /**
     * Validates availability slot parameters.
     *
     * @param dayOfWeek day of the week
     * @param startTime slot start time
     * @param endTime slot end time
     * @throws ValidationException if times are invalid
     */
    public static void validateAvailability(DayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime)
            throws ValidationException {
        Map<String, String> errors = new HashMap<>();

        if (dayOfWeek == null) {
            errors.put("dayOfWeek", "Day of week is required");
        }
        if (startTime == null) {
            errors.put("startTime", "Start time is required");
        }
        if (endTime == null) {
            errors.put("endTime", "End time is required");
        }
        if (startTime != null && endTime != null && !endTime.isAfter(startTime)) {
            errors.put("endTime", "End time must be after start time");
        }

        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
    }

    /**
     * Validates review submission parameters.
     *
     * @param rating review rating (1 to 5)
     * @param comment optional feedback comment
     * @throws ValidationException if rating or comment length is invalid
     */
    public static void validateReview(int rating, String comment) throws ValidationException {
        Map<String, String> errors = new HashMap<>();

        if (rating < 1 || rating > 5) {
            errors.put("rating", "Rating must be between 1 and 5");
        }
        if (comment != null && comment.length() > 500) {
            errors.put("comment", "Review comment cannot exceed 500 characters");
        }

        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
    }

    /**
     * Validates avatar image upload file header bytes and size.
     *
     * @param filename uploaded file name
     * @param sizeBytes size of uploaded file
     * @param headerBytes initial 8-12 bytes of the file for magic number check
     * @throws ValidationException if file is not an allowed image type or exceeds 2MB
     */
    public static void validateImageUpload(String filename, long sizeBytes, byte[] headerBytes)
            throws ValidationException {
        Map<String, String> errors = new HashMap<>();

        if (sizeBytes > 2 * 1024 * 1024) {
            errors.put("avatar", "Image size must not exceed 2 MB");
            throw new ValidationException(errors);
        }

        if (filename == null || filename.trim().isEmpty()) {
            errors.put("avatar", "File name is missing");
            throw new ValidationException(errors);
        }

        String lower = filename.trim().toLowerCase();
        boolean hasValidExt = lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png");
        if (!hasValidExt) {
            errors.put("avatar", "Only JPG and PNG images are supported");
            throw new ValidationException(errors);
        }

        if (headerBytes == null || headerBytes.length < 3) {
            errors.put("avatar", "Invalid or corrupted image file");
            throw new ValidationException(errors);
        }

        // JPEG magic bytes: FF D8 FF
        boolean isJpeg = (headerBytes[0] == (byte) 0xFF && headerBytes[1] == (byte) 0xD8 && headerBytes[2] == (byte) 0xFF);

        // PNG magic bytes: 89 50 4E 47 0D 0A 1A 0A
        boolean isPng = headerBytes.length >= 8
                && (headerBytes[0] == (byte) 0x89
                && headerBytes[1] == (byte) 0x50
                && headerBytes[2] == (byte) 0x4E
                && headerBytes[3] == (byte) 0x47
                && headerBytes[4] == (byte) 0x0D
                && headerBytes[5] == (byte) 0x0A
                && headerBytes[6] == (byte) 0x1A
                && headerBytes[7] == (byte) 0x0A);

        if (!isJpeg && !isPng) {
            errors.put("avatar", "The uploaded file is not a valid JPEG or PNG image");
            throw new ValidationException(errors);
        }
    }
}
