package io.github.ozkanpakdil.grepwise.service;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility for redacting sensitive information from strings.
 * Patterns for masking are sourced from redaction.json (CUSTOM_PATTERNS).
 * Keys are used only to detect keyword presence and to mask metadata values when the key name itself is sensitive.
 */
public final class RedactionUtil {
    // Initialize to neutral no-op patterns until refresh() is called.
    private static volatile Pattern WORD_ONLY_PATTERN = Pattern.compile("a^", Pattern.CASE_INSENSITIVE); // matches nothing
    private static volatile Pattern[] CUSTOM_PATTERNS = new Pattern[0];

    private static Pattern buildWordOnlyPattern(String[] keys) {
        String keyAlt = String.join("|", keys);
        return Pattern.compile("(?i)(" + keyAlt + ")");
    }

    /**
     * Refresh with keys and custom regex patterns loaded from JSON.
     * Custom patterns must contain a capturing group for the secret part and optionally a leading group for the prefix.
     * If only one group exists, the whole match will be replaced.
     */
    public static void refresh(Collection<String> keys, Collection<String> patterns) {
        // keys -> detection only (no masking regexes generated here)
        if (keys != null && !keys.isEmpty()) {
            String[] arr = keys.stream().filter(Objects::nonNull).map(String::trim).filter(s -> !s.isEmpty()).toArray(String[]::new);
            if (arr.length > 0) {
                WORD_ONLY_PATTERN = buildWordOnlyPattern(arr);
            }
        }
        // compile custom patterns (used for masking)
        if (patterns != null && !patterns.isEmpty()) {
            CUSTOM_PATTERNS = patterns.stream()
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(p -> {
                        try {
                            return Pattern.compile(p);
                        } catch (Exception e) {
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .toArray(Pattern[]::new);
        } else {
            CUSTOM_PATTERNS = new Pattern[0];
        }
    }

    private RedactionUtil() {
    }

    public static boolean containsSensitive(CharSequence text) {
        if (text == null) return false;
        if (WORD_ONLY_PATTERN.matcher(text).find()) return true;
        for (Pattern p : CUSTOM_PATTERNS) {
            if (p.matcher(text).find()) return true;
        }
        return false;
    }

    public static String redactLine(String line, String mask) {
        if (line == null) return null;
        String redacted = line;
        // Apply only custom patterns provided by configuration
        for (Pattern p : CUSTOM_PATTERNS) {
            Matcher m = p.matcher(redacted);
            StringBuffer sb = new StringBuffer();
            boolean found = false;
            while (m.find()) {
                found = true;
                String replacement;
                if (m.groupCount() >= 2 && m.group(1) != null) {
                    replacement = Matcher.quoteReplacement(m.group(1) + mask);
                } else {
                    replacement = Matcher.quoteReplacement(mask);
                }
                m.appendReplacement(sb, replacement);
            }
            if (found) {
                m.appendTail(sb);
                redacted = sb.toString();
            }
        }
        // If we still have plain occurrences without a value pattern, leave as-is; requirement focuses on values
        return redacted;
    }

    public static String redactIfSensitive(String text, String mask) {
        if (!containsSensitive(text)) return text;
        return redactLine(text, mask);
    }

    public static void redactMetadataValues(Map<String, String> metadata, String mask) {
        if (metadata == null) return;
        for (Map.Entry<String, String> e : metadata.entrySet()) {
            String v = e.getValue();
            if (v != null && containsSensitive(v)) {
                e.setValue(redactLine(v, mask));
            }
            // Also check key name if it is password-like
            String k = e.getKey();
            if (k != null && WORD_ONLY_PATTERN.matcher(k).find() && v != null) {
                e.setValue(mask);
            }
        }
    }
}
