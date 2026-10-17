package com.skillswap.exception;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Exception thrown when user input fails validation rules, carrying field-specific errors.
 */
public class ValidationException extends SkillSwapException {

    private static final long serialVersionUID = 1L;
    private final Map<String, String> fieldErrors;

    /**
     * Constructs a ValidationException with a general message.
     *
     * @param message the error message
     */
    public ValidationException(String message) {
        super(message);
        this.fieldErrors = new HashMap<>();
    }

    /**
     * Constructs a ValidationException with a map of field-specific errors.
     *
     * @param fieldErrors map of field names to error messages
     */
    public ValidationException(Map<String, String> fieldErrors) {
        super(fieldErrors.isEmpty() ? "Validation failed" : fieldErrors.values().iterator().next());
        this.fieldErrors = new HashMap<>(fieldErrors);
    }

    /**
     * Constructs a ValidationException with a general message and field errors map.
     *
     * @param message the error message
     * @param fieldErrors map of field names to error messages
     */
    public ValidationException(String message, Map<String, String> fieldErrors) {
        super(message);
        this.fieldErrors = fieldErrors != null ? new HashMap<>(fieldErrors) : new HashMap<>();
    }

    /**
     * Returns an unmodifiable map of field-specific error messages.
     *
     * @return map of field name to error message
     */
    public Map<String, String> getFieldErrors() {
        return Collections.unmodifiableMap(fieldErrors);
    }

    /**
     * Checks if this exception carries any field-specific errors.
     *
     * @return true if field errors exist, false otherwise
     */
    public boolean hasFieldErrors() {
        return !fieldErrors.isEmpty();
    }
}
