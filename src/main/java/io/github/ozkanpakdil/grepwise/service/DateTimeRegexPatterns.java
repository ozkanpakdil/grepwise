package io.github.ozkanpakdil.grepwise.service;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DateTimeRegexPatterns {

    // Simplified: only match ISO-8601 timestamps (with or without timezone) to reduce complexity
    // Examples matched: 2025-08-30T09:44:41.814Z, 2025-08-30T09:44:41Z, 2025-08-30T09:44:41.814+05:30, 2025-08-30T09:44:41.814, 2025-08-30T09:44:41
    private static final List<String> DATE_TIME_PATTERNS = List.of(
            // ISO 8601 with optional fractional seconds and optional timezone (Z or +HH:mm or +HHmm)
            "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(?:\\.\\d{1,9})?(?:Z|[+-]\\d{2}:?\\d{2})?"
    );
    // Development logging flag: show parse errors when -Dgrepwise.dev=true, otherwise keep quiet to avoid spam
    private static final boolean DEV_LOG = Boolean.parseBoolean(System.getProperty("grepwise.dev", "false"));

    public static void main(String[] args) {
        // Test with sample text
        String testText = "Today is 2023-12-25 14:30:45 and yesterday was Dec 24, 2023 2:30 PM. " +
                "The timestamp 1703520645 shows 25/12/2023 14:30:45.123 in logs.";

        System.out.println("=== EXTRACTING AND CONVERTING TO TIMESTAMPS ===");
        System.out.println("Text: " + testText);
        System.out.println();

        // Extract first date and convert to timestamp
        long firstTimestamp = extractDateTimeToTimestamp(testText);
        if (firstTimestamp != -1) {
            System.out.println("First date found: " + extractFirstDateTime(testText));
            System.out.println("Timestamp: " + firstTimestamp);
            System.out.println("Converted back: " + timestampToDateString(firstTimestamp));
            System.out.println();
        }

        // Extract all dates and convert to timestamps
        System.out.println("All dates and timestamps:");
        List<String> allDates = extractAllDateTimes(testText);
        List<Long> allTimestamps = extractAllDateTimesToTimestamps(testText);

        for (String dateStr : allDates) {
            long timestamp = convertToTimestamp(dateStr);
            System.out.printf("Date: %-20s -> Timestamp: %-13s -> Back: %s%n",
                    dateStr,
                    timestamp != -1 ? timestamp : "FAILED",
                    timestamp != -1 ? timestampToDateString(timestamp) : "N/A");
        }

        System.out.println("\n" + "=".repeat(80));
        extractDateTimes(testText);
    }

    public static void extractDateTimes(String text) {
        System.out.println("Extracting date-times from: " + text);
        System.out.println("=" + "=".repeat(80));

        for (int i = 0; i < DATE_TIME_PATTERNS.size(); i++) {
            String pattern = DATE_TIME_PATTERNS.get(i);
            Pattern compiledPattern = Pattern.compile(pattern);
            Matcher matcher = compiledPattern.matcher(text);

            if (matcher.find()) {
                System.out.printf("Pattern %2d: %-70s -> %s%n",
                        i + 1, pattern, matcher.group());

                // Reset matcher to find all matches
                matcher.reset();
                System.out.print("All matches: ");
                while (matcher.find()) {
                    System.out.print("[" + matcher.group() + "] ");
                }
                System.out.println();
                System.out.println();
            }
        }
    }

    // Utility method to try all patterns and return first match
    public static String extractFirstDateTime(String text) {
        if (text == null || text.isEmpty()) {
            return null;
        }
        for (String pattern : DATE_TIME_PATTERNS) {
            Pattern compiledPattern = Pattern.compile(pattern);
            Matcher matcher = compiledPattern.matcher(text);
            if (matcher.find()) {
                return matcher.group();
            }
        }
        return null;
    }

    // Utility method to get all date-time matches from text
    public static List<String> extractAllDateTimes(String text) {
        List<String> matches = new java.util.ArrayList<>();
        for (String pattern : DATE_TIME_PATTERNS) {
            Pattern compiledPattern = Pattern.compile(pattern);
            Matcher matcher = compiledPattern.matcher(text);
            while (matcher.find()) {
                String match = matcher.group();
                if (!matches.contains(match)) { // Avoid duplicates
                    matches.add(match);
                }
            }
        }
        return matches;
    }

    /**
     * Extract date string and convert to timestamp (milliseconds since epoch)
     *
     * @param text The text to search for date-time
     * @return timestamp in milliseconds, or -1 if no date found
     */
    public static long extractDateTimeToTimestamp(String text) {
        String dateStr = extractFirstDateTime(text);
        if (dateStr == null) {
            return -1;
        }
        return convertToTimestamp(dateStr);
    }

    /**
     * Convert date string to timestamp using multiple format patterns
     *
     * @param dateStr The date string to parse
     * @return timestamp in milliseconds, or -1 if parsing fails
     */
    public static long convertToTimestamp(String dateStr) {
        if (dateStr == null) return -1;
        String s = dateStr.trim();

        // Handle Unix timestamps (already numbers)
        if (s.matches("\\d{10}")) {
            return Long.parseLong(s) * 1000; // seconds to ms
        }
        if (s.matches("\\d{13}")) {
            return Long.parseLong(s); // already ms
        }

        try {
            // If timezone present (Z or +HH:mm or +HHmm), parse as OffsetDateTime/Instant
            if (s.endsWith("Z") || s.matches(".*[+-]\\d{2}:?\\d{2}$")) {
                String normalized = s;
                // Normalize +HHmm to +HH:mm so that standard formatters accept it
                if (normalized.matches(".*[+-]\\d{4}$")) {
                    normalized = normalized.substring(0, normalized.length() - 5)
                            + normalized.substring(normalized.length() - 5, normalized.length() - 2)
                            + ":"
                            + normalized.substring(normalized.length() - 2);
                }
                try {
                    return java.time.Instant.parse(normalized).toEpochMilli();
                } catch (Exception ex) {
                    return java.time.OffsetDateTime.parse(normalized, java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                            .toInstant().toEpochMilli();
                }
            }

            // Otherwise, treat as ISO local datetime with optional fraction, interpret as UTC per requirement
            java.time.format.DateTimeFormatter isoLocal = java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME;
            java.time.LocalDateTime ldt = java.time.LocalDateTime.parse(s, isoLocal);
            return ldt.toInstant(java.time.ZoneOffset.UTC).toEpochMilli();
        } catch (Exception ex) {
            if (DEV_LOG) {
                System.err.println("Failed to parse date: " + s + " - " + ex.getMessage());
            }
            return -1;
        }
    }

    /**
     * Extract all date strings and convert to timestamps
     *
     * @param text The text to search for date-times
     * @return List of timestamps in milliseconds
     */
    public static List<Long> extractAllDateTimesToTimestamps(String text) {
        List<String> dateStrings = extractAllDateTimes(text);
        List<Long> timestamps = new java.util.ArrayList<>();

        for (String dateStr : dateStrings) {
            long timestamp = convertToTimestamp(dateStr);
            if (timestamp != -1) {
                timestamps.add(timestamp);
            }
        }
        return timestamps;
    }

    /**
     * Convert timestamp back to readable date string
     *
     * @param timestamp milliseconds since epoch
     * @return formatted date string
     */
    public static String timestampToDateString(long timestamp) {
        Date date = new Date(timestamp);
        return new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(date);
    }
}