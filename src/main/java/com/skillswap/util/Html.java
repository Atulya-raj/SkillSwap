package com.skillswap.util;

/**
 * HTML escaping and sanitization utilities.
 */
public final class Html {

    private Html() {
    }

    /**
     * Escapes special characters for safe inclusion in HTML body or attribute contexts.
     *
     * @param input the raw string
     * @return HTML-escaped string, or empty string if input is null
     */
    public static String escape(String input) {
        if (input == null) {
            return "";
        }
        StringBuilder out = new StringBuilder(input.length() + 16);
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            switch (c) {
                case '&' -> out.append("&amp;");
                case '<' -> out.append("&lt;");
                case '>' -> out.append("&gt;");
                case '"' -> out.append("&quot;");
                case '\'' -> out.append("&#39;");
                default -> out.append(c);
            }
        }
        return out.toString();
    }
}
