package com.skillswap.io;

import com.skillswap.model.SessionRequest;
import com.skillswap.model.User;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * RFC 4180-compliant CSV exporter with UTF-8 BOM prefix for Microsoft Excel and spreadsheet compatibility.
 *
 * @param <T> domain entity type
 */
public class CsvExporter<T> implements Exporter<T> {

    private static final byte[] UTF8_BOM = new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final String[] headers;
    private final Function<T, String[]> rowExtractor;

    /**
     * Constructs a CsvExporter with specified headers and a row mapping function.
     *
     * @param headers array of column header names
     * @param rowExtractor function mapping each entity to an array of column values
     */
    public CsvExporter(String[] headers, Function<T, String[]> rowExtractor) {
        this.headers = Objects.requireNonNull(headers, "headers must not be null");
        this.rowExtractor = Objects.requireNonNull(rowExtractor, "rowExtractor must not be null");
    }

    @Override
    public void export(List<T> items, OutputStream out) throws IOException {
        // Write UTF-8 Byte Order Mark (BOM) so Excel opens UTF-8 encoded text correctly
        out.write(UTF8_BOM);

        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8));

        // Write header line
        writeRow(writer, headers);

        // Write data lines
        if (items != null) {
            for (T item : items) {
                if (item != null) {
                    String[] row = rowExtractor.apply(item);
                    writeRow(writer, row);
                }
            }
        }
        writer.flush();
    }

    /**
     * Formats and writes a single row of cells with commas and CRLF line ending.
     */
    private void writeRow(BufferedWriter writer, String[] values) throws IOException {
        if (values == null) {
            writer.write("\r\n");
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(escapeField(values[i]));
        }
        sb.append("\r\n");
        writer.write(sb.toString());
    }

    /**
     * Escapes a single CSV field according to RFC 4180.
     * Quotes fields containing commas, double quotes, or newlines, doubling any internal quotes.
     *
     * @param field raw string value
     * @return properly quoted and escaped field
     */
    public static String escapeField(String field) {
        if (field == null) {
            return "";
        }
        boolean mustQuote = field.contains(",") || field.contains("\"")
                || field.contains("\n") || field.contains("\r")
                || field.startsWith(" ") || field.endsWith(" ");

        if (!mustQuote) {
            return field;
        }

        StringBuilder sb = new StringBuilder();
        sb.append('"');
        for (int i = 0; i < field.length(); i++) {
            char c = field.charAt(i);
            if (c == '"') {
                sb.append("\"\"");
            } else {
                sb.append(c);
            }
        }
        sb.append('"');
        return sb.toString();
    }

    /**
     * Factory producing a CsvExporter for student session history.
     *
     * @param currentUserId the student whose history is being exported
     * @return configured CsvExporter for SessionRequest entities
     */
    public static CsvExporter<SessionRequest> forSessionHistory(long currentUserId) {
        String[] headers = {
                "Request ID",
                "Role",
                "Partner",
                "Skill",
                "Proposed Start",
                "Duration (min)",
                "Mode",
                "Meeting Details",
                "Cost (Credits)",
                "Status",
                "Created At"
        };

        Function<SessionRequest, String[]> extractor = req -> {
            boolean isLearner = req.getLearnerId() == currentUserId;
            String role = isLearner ? "LEARNER" : "MENTOR";
            String partner = isLearner ? req.getProviderName() : req.getLearnerName();
            String startStr = req.getProposedStart() != null ? req.getProposedStart().format(DATE_TIME_FORMATTER) : "";
            String createdStr = req.getCreatedAt() != null ? req.getCreatedAt().format(DATE_TIME_FORMATTER) : "";

            return new String[]{
                    String.valueOf(req.getId()),
                    role,
                    partner != null ? partner : ("User #" + (isLearner ? req.getProviderId() : req.getLearnerId())),
                    req.getSkillName() != null ? req.getSkillName() : ("Skill #" + req.getSkillId()),
                    startStr,
                    String.valueOf(req.getDurationMinutes()),
                    req.getMode() != null ? req.getMode().name() : "",
                    req.getMeetingDetails() != null ? req.getMeetingDetails() : "",
                    String.valueOf(req.getCostCredits()),
                    req.getStatus() != null ? req.getStatus().name() : "",
                    createdStr
            };
        };

        return new CsvExporter<>(headers, extractor);
    }

    /**
     * Factory producing a CsvExporter for administrative user roster.
     *
     * @return configured CsvExporter for User entities
     */
    public static CsvExporter<User> forUsers() {
        String[] headers = {
                "User ID",
                "Full Name",
                "Email",
                "Role",
                "Department",
                "Study Year",
                "Credits",
                "Average Rating",
                "Rating Count",
                "Sessions Taught",
                "Sessions Learned",
                "Reliability Strikes",
                "Active Status",
                "Created At",
                "Last Login"
        };

        Function<User, String[]> extractor = u -> {
            String createdStr = u.getCreatedAt() != null ? u.getCreatedAt().format(DATE_TIME_FORMATTER) : "";
            String loginStr = u.getLastLoginAt() != null ? u.getLastLoginAt().format(DATE_TIME_FORMATTER) : "Never";

            return new String[]{
                    String.valueOf(u.getId()),
                    u.getFullName(),
                    u.getEmail(),
                    u.getRole() != null ? u.getRole().name() : "",
                    u.getDepartment() != null ? u.getDepartment() : "",
                    u.getStudyYear() != null ? String.valueOf(u.getStudyYear()) : "",
                    String.valueOf(u.getCredits()),
                    u.getAvgRating() != null ? u.getAvgRating().toString() : "0.00",
                    String.valueOf(u.getRatingCount()),
                    String.valueOf(u.getSessionsTaught()),
                    String.valueOf(u.getSessionsLearned()),
                    String.valueOf(u.getReliabilityStrikes()),
                    u.isActive() ? "ACTIVE" : "INACTIVE",
                    createdStr,
                    loginStr
            };
        };

        return new CsvExporter<>(headers, extractor);
    }
}
