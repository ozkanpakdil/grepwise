package io.github.ozkanpakdil.grepwise.service;

import org.apache.commons.lang3.time.DateUtils;

import java.text.ParseException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DateTimeRegexPatterns {

    // List of common date-time regex patterns
    private static final List<String> DATE_TIME_PATTERNS = Arrays.asList(
            // ISO 8601 formats
            "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(?:\\.\\d{3})?Z?",                    // 2023-12-25T14:30:45.123Z
            "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(?:[+-]\\d{2}:\\d{2})?",             // 2023-12-25T14:30:45+05:30
            "\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}(?:\\.\\d{3})?",                     // 2023-12-25 14:30:45.123

            // Standard date formats with time
            "\\d{4}/\\d{1,2}/\\d{1,2} \\d{1,2}:\\d{2}:\\d{2}",                             // 2023/12/25 14:30:45
            "\\d{1,2}/\\d{1,2}/\\d{4} \\d{1,2}:\\d{2}:\\d{2}",                             // 25/12/2023 14:30:45
            "\\d{1,2}-\\d{1,2}-\\d{4} \\d{1,2}:\\d{2}:\\d{2}",                             // 25-12-2023 14:30:45
            "\\d{4}-\\d{1,2}-\\d{1,2} \\d{1,2}:\\d{2}:\\d{2}",                             // 2023-12-25 14:30:45

            // Date formats with AM/PM
            "\\d{1,2}/\\d{1,2}/\\d{4} \\d{1,2}:\\d{2}:\\d{2} (?:AM|PM)",                   // 12/25/2023 2:30:45 PM
            "\\d{4}/\\d{1,2}/\\d{1,2} \\d{1,2}:\\d{2}:\\d{2} (?:AM|PM)",                   // 2023/12/25 2:30:45 PM
            "\\d{1,2}-\\d{1,2}-\\d{4} \\d{1,2}:\\d{2}:\\d{2} (?:AM|PM)",                   // 25-12-2023 2:30:45 PM

            // Date formats with hours and minutes only
            "\\d{4}/\\d{1,2}/\\d{1,2} \\d{1,2}:\\d{2}",                                    // 2023/12/25 14:30
            "\\d{1,2}/\\d{1,2}/\\d{4} \\d{1,2}:\\d{2}",                                    // 25/12/2023 14:30
            "\\d{1,2}-\\d{1,2}-\\d{4} \\d{1,2}:\\d{2}",                                    // 25-12-2023 14:30
            "\\d{4}-\\d{1,2}-\\d{1,2} \\d{1,2}:\\d{2}",                                    // 2023-12-25 14:30

            // Date formats with AM/PM (hours:minutes only)
            "\\d{1,2}/\\d{1,2}/\\d{4} \\d{1,2}:\\d{2} (?:AM|PM)",                          // 12/25/2023 2:30 PM
            "\\d{4}/\\d{1,2}/\\d{1,2} \\d{1,2}:\\d{2} (?:AM|PM)",                          // 2023/12/25 2:30 PM
            "\\d{1,2}-\\d{1,2}-\\d{4} \\d{1,2}:\\d{2} (?:AM|PM)",                          // 25-12-2023 2:30 PM

            // Date only formats
            "\\d{4}/\\d{1,2}/\\d{1,2}",                                                     // 2023/12/25
            "\\d{1,2}/\\d{1,2}/\\d{4}",                                                     // 12/25/2023
            "\\d{4}-\\d{1,2}-\\d{1,2}",                                                     // 2023-12-25
            "\\d{1,2}-\\d{1,2}-\\d{4}",                                                     // 25-12-2023
            "\\d{1,2}\\.\\d{1,2}\\.\\d{4}",                                                 // 25.12.2023
            "\\d{4}\\.\\d{1,2}\\.\\d{1,2}",                                                 // 2023.12.25

            // Month name formats
            "(?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec) \\d{1,2}, \\d{4}",         // Dec 25, 2023
            "\\d{1,2} (?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec) \\d{4}",          // 25 Dec 2023
            "(?:January|February|March|April|May|June|July|August|September|October|November|December) \\d{1,2}, \\d{4}", // December 25, 2023
            "\\d{1,2} (?:January|February|March|April|May|June|July|August|September|October|November|December) \\d{4}",  // 25 December 2023

            // Month name with time
            "(?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec) \\d{1,2}, \\d{4} \\d{1,2}:\\d{2}:\\d{2}", // Dec 25, 2023 14:30:45
            "\\d{1,2} (?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec) \\d{4} \\d{1,2}:\\d{2}:\\d{2}",  // 25 Dec 2023 14:30:45
            "(?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec) \\d{1,2}, \\d{4} \\d{1,2}:\\d{2}:\\d{2} (?:AM|PM)", // Dec 25, 2023 2:30:45 PM

            // European formats
            "\\d{1,2}/\\d{1,2}/\\d{2}",                                                     // 25/12/23
            "\\d{1,2}-\\d{1,2}-\\d{2}",                                                     // 25-12-23
            "\\d{2}/\\d{1,2}/\\d{1,2}",                                                     // 23/12/25

            // Compact formats
            "\\d{8}",                                                                        // 20231225
            "\\d{6}",                                                                        // 231225
            "\\d{14}",                                                                       // 20231225143045
            "\\d{12}",                                                                       // 231225143045

            // Time only formats
            "\\d{1,2}:\\d{2}:\\d{2}",                                                       // 14:30:45
            "\\d{1,2}:\\d{2}",                                                              // 14:30
            "\\d{1,2}:\\d{2}:\\d{2} (?:AM|PM)",                                             // 2:30:45 PM
            "\\d{1,2}:\\d{2} (?:AM|PM)",                                                    // 2:30 PM

            // RFC 2822 format
            "(?:Mon|Tue|Wed|Thu|Fri|Sat|Sun), \\d{1,2} (?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec) \\d{4} \\d{2}:\\d{2}:\\d{2} [+-]\\d{4}", // Mon, 25 Dec 2023 14:30:45 +0000

            // Log formats
            "\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2},\\d{3}",                           // 2023-12-25 14:30:45,123
            "\\d{2}/\\d{2}/\\d{4} \\d{2}:\\d{2}:\\d{2}",                                   // 12/25/2023 14:30:45

            // Unix timestamp
            "\\d{10}",                                                                       // 1703520645 (Unix timestamp)
            "\\d{13}"                                                                        // 1703520645123 (Unix timestamp with milliseconds)
    );

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

    // Common date format patterns for parsing with DateUtils
    private static final String[] PARSE_PATTERNS = {
            // ISO 8601 formats
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ssXXX", "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd HH:mm:ss.SSS", "yyyy-MM-dd HH:mm:ss",

            // Standard formats
            "yyyy/MM/dd HH:mm:ss", "dd/MM/yyyy HH:mm:ss", "MM/dd/yyyy HH:mm:ss",
            "yyyy-MM-dd HH:mm:ss", "dd-MM-yyyy HH:mm:ss",

            // AM/PM formats
            "MM/dd/yyyy hh:mm:ss a", "yyyy/MM/dd hh:mm:ss a", "dd-MM-yyyy hh:mm:ss a",

            // Date with time (no seconds)
            "yyyy/MM/dd HH:mm", "dd/MM/yyyy HH:mm", "MM/dd/yyyy HH:mm",
            "yyyy-MM-dd HH:mm", "dd-MM-yyyy HH:mm",

            // AM/PM (no seconds)
            "MM/dd/yyyy hh:mm a", "yyyy/MM/dd hh:mm a", "dd-MM-yyyy hh:mm a",

            // Date only formats
            "yyyy/MM/dd", "dd/MM/yyyy", "MM/dd/yyyy", "yyyy-MM-dd", "dd-MM-yyyy",
            "dd.MM.yyyy", "yyyy.MM.dd",

            // Month name formats
            "MMM dd, yyyy", "dd MMM yyyy", "MMMM dd, yyyy", "dd MMMM yyyy",
            "MMM dd, yyyy HH:mm:ss", "dd MMM yyyy HH:mm:ss",
            "MMM dd, yyyy hh:mm:ss a",

            // Compact formats
            "yyyyMMdd", "yyMMdd", "yyyyMMddHHmmss", "yyMMddHHmmss",

            // Time only
            "HH:mm:ss", "HH:mm", "hh:mm:ss a", "hh:mm a",

            // Log formats
            "yyyy-MM-dd HH:mm:ss,SSS", "dd/MM/yyyy HH:mm:ss"
    };

    /**
     * Extract date string and convert to timestamp (milliseconds since epoch)
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
     * @param dateStr The date string to parse
     * @return timestamp in milliseconds, or -1 if parsing fails
     */
    public static long convertToTimestamp(String dateStr) {
        // Handle Unix timestamps (already numbers)
        if (dateStr.matches("\\d{10}")) {
            return Long.parseLong(dateStr) * 1000; // Convert seconds to milliseconds
        }
        if (dateStr.matches("\\d{13}")) {
            return Long.parseLong(dateStr); // Already in milliseconds
        }

        try {
            Date date = DateUtils.parseDate(dateStr, PARSE_PATTERNS);
            return date.getTime();
        } catch (ParseException e) {
            System.err.println("Failed to parse date: " + dateStr + " - " + e.getMessage());
            return -1;
        }
    }

    /**
     * Extract all date strings and convert to timestamps
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
     * @param timestamp milliseconds since epoch
     * @return formatted date string
     */
    public static String timestampToDateString(long timestamp) {
        Date date = new Date(timestamp);
        return new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(date);
    }
}