package io.github.ozkanpakdil.grepwise.service;

import io.github.ozkanpakdil.grepwise.model.LogDirectoryConfig;
import io.github.ozkanpakdil.grepwise.model.LogEntry;
import io.github.ozkanpakdil.grepwise.repository.LogDirectoryConfigRepository;
import io.github.ozkanpakdil.grepwise.repository.LogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Service for scanning log files in specified directories.
 */
@Service
public class LogScannerService {
    private static final Logger logger = LoggerFactory.getLogger(LogScannerService.class);

    // Common log patterns
    private static final Pattern LOG4J_PATTERN = Pattern.compile("(\\d{4}-\\d{2}-\\d{2}\\s\\d{2}:\\d{2}:\\d{2},\\d{3})\\s+(\\w+)\\s+\\[([^\\]]+)\\]\\s+(.*)");
    private static final Pattern SIMPLE_PATTERN = Pattern.compile("(\\d{4}-\\d{2}-\\d{2}\\s\\d{2}:\\d{2}:\\d{2})\\s+(\\w+)\\s+(.*)");

    private final LogRepository logRepository;
    private final LogDirectoryConfigRepository configRepository;

    public LogScannerService(LogRepository logRepository, LogDirectoryConfigRepository configRepository) {
        this.logRepository = logRepository;
        this.configRepository = configRepository;
        logger.info("LogScannerService initialized");
    }

    /**
     * Scan all configured directories for log files.
     * This method is scheduled to run periodically.
     */
    @Scheduled(fixedDelay = 60000) // Run every minute
    public void scanAllDirectories() {
        logger.info("Starting scheduled scan of all log directories");
        List<LogDirectoryConfig> configs = configRepository.findAll();
        logger.info("Found {} log directory configurations", configs.size());

        int scannedCount = 0;
        for (LogDirectoryConfig config : configs) {
            if (config.isEnabled()) {
                try {
                    int processed = scanDirectory(config);
                    if (processed > 0) {
                        scannedCount++;
                    }
                } catch (Exception e) {
                    logger.error("Error scanning directory: " + config.getDirectoryPath(), e);
                }
            }
        }
        logger.info("Completed scanning {} directories", scannedCount);
    }

    /**
     * Scan a specific directory for log files.
     *
     * @param config The directory configuration
     * @return The number of log entries processed
     */
    public int scanDirectory(LogDirectoryConfig config) {
        String directoryPath = config.getDirectoryPath();
        String filePattern = config.getFilePattern();

        logger.info("Scanning directory: {} with pattern: {}", directoryPath, filePattern);

        Path dir = Paths.get(directoryPath);
        if (!Files.exists(dir) || !Files.isDirectory(dir)) {
            logger.error("Directory does not exist or is not a directory: {}", directoryPath);
            return 0;
        }

        PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + filePattern);

        List<Path> logFiles;
        try (Stream<Path> stream = Files.list(dir)) {
            logFiles = stream
                    .filter(Files::isRegularFile)
                    .filter(path -> {
                        boolean matches = matcher.matches(path.getFileName());
                        logger.debug("File: {}, matches pattern: {}", path.getFileName(), matches);
                        return matches;
                    })
                    .collect(Collectors.toList());

            logger.info("Found {} log files matching pattern in directory: {}", logFiles.size(), directoryPath);
        } catch (IOException e) {
            logger.error("Error listing files in directory: {}", directoryPath, e);
            return 0;
        }

        int totalProcessed = 0;
        for (Path logFile : logFiles) {
            try {
                int processed = processLogFile(logFile.toFile());
                totalProcessed += processed;
                logger.info("Processed {} log entries from file: {}", processed, logFile);
            } catch (Exception e) {
                logger.error("Error processing log file: {}", logFile, e);
            }
        }

        return totalProcessed;
    }

    /**
     * Process a log file and extract log entries.
     *
     * @param file The log file to process
     * @return The number of log entries processed
     */
    private int processLogFile(File file) {
        logger.debug("Processing log file: {}", file.getAbsolutePath());
        List<LogEntry> logEntries = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            StringBuilder currentEntry = new StringBuilder();
            LogEntry currentLogEntry = null;
            int lineCount = 0;
            int matchedLines = 0;

            while ((line = reader.readLine()) != null) {
                lineCount++;
                // Check if this line starts a new log entry
                LogEntry newEntry = parseLogLine(line, file.getName());

                if (newEntry != null) {
                    matchedLines++;
                    logger.debug("Line {} matched a log pattern: {}", lineCount, line.substring(0, Math.min(line.length(), 100)));

                    // If we have a previous entry, save it
                    if (currentLogEntry != null) {
                        currentLogEntry.setRawContent(currentEntry.toString());
                        logEntries.add(currentLogEntry);
                    }

                    // Start a new entry
                    currentLogEntry = newEntry;
                    currentEntry = new StringBuilder(line);
                } else if (currentLogEntry != null) {
                    // This is a continuation of the current entry
                    currentEntry.append("\n").append(line);

                    // Update the message to include this line
                    String currentMessage = currentLogEntry.getMessage();
                    currentLogEntry.setMessage(currentMessage + "\n" + line);
                } else {
                    logger.debug("Line {} did not match any log pattern: {}", lineCount, line.substring(0, Math.min(line.length(), 100)));
                }
            }

            // Don't forget the last entry
            if (currentLogEntry != null) {
                currentLogEntry.setRawContent(currentEntry.toString());
                logEntries.add(currentLogEntry);
            }

            logger.debug("Processed {} lines in file, matched {} log entries", lineCount, matchedLines);
        } catch (IOException e) {
            logger.error("Error reading log file: {}", file, e);
            return 0;
        }

        // Save all log entries to the repository
        int savedCount = logRepository.saveAll(logEntries);
        logger.info("Saved {} log entries from file: {}", savedCount, file.getName());
        return savedCount;
    }

    /**
     * Parse a log line and extract a log entry.
     *
     * @param line The log line to parse
     * @param source The source of the log (filename)
     * @return A LogEntry if the line is the start of a log entry, null otherwise
     */
    private LogEntry parseLogLine(String line, String source) {
        // Try different log patterns
        Matcher log4jMatcher = LOG4J_PATTERN.matcher(line);
        if (log4jMatcher.matches()) {
            String timestamp = log4jMatcher.group(1);
            String level = log4jMatcher.group(2);
            String thread = log4jMatcher.group(3);
            String message = log4jMatcher.group(4);

            logger.debug("Matched LOG4J pattern: timestamp={}, level={}, thread={}, message={}", 
                    timestamp, level, thread, message.substring(0, Math.min(message.length(), 50)));

            Map<String, String> metadata = new HashMap<>();
            metadata.put("thread", thread);

            long recordTime = parseTimestamp(timestamp);
            return new LogEntry(
                    UUID.randomUUID().toString(),
                    System.currentTimeMillis(), // Entry time (when the log was scanned)
                    recordTime, // Record time (from the log line)
                    level,
                    message,
                    source,
                    metadata,
                    line
            );
        }

        Matcher simpleMatcher = SIMPLE_PATTERN.matcher(line);
        if (simpleMatcher.matches()) {
            String timestamp = simpleMatcher.group(1);
            String level = simpleMatcher.group(2);
            String message = simpleMatcher.group(3);

            logger.debug("Matched SIMPLE pattern: timestamp={}, level={}, message={}", 
                    timestamp, level, message.substring(0, Math.min(message.length(), 50)));

            long recordTime = parseTimestamp(timestamp);
            return new LogEntry(
                    UUID.randomUUID().toString(),
                    System.currentTimeMillis(), // Entry time (when the log was scanned)
                    recordTime, // Record time (from the log line)
                    level,
                    message,
                    source,
                    new HashMap<>(),
                    line
            );
        }

        // If we get here, the line didn't match any pattern
        logger.trace("Line did not match any pattern: {}", line.substring(0, Math.min(line.length(), 100)));
        return null;
    }

    /**
     * Parse a timestamp string into a Unix timestamp (milliseconds since epoch).
     *
     * @param timestamp The timestamp string
     * @return The Unix timestamp
     */
    private long parseTimestamp(String timestamp) {
        try {
            // Try to parse different timestamp formats
            if (timestamp.matches("\\d{4}-\\d{2}-\\d{2}\\s\\d{2}:\\d{2}:\\d{2},\\d{3}")) {
                // Format: 2023-01-01 12:34:56,789
                String[] parts = timestamp.split(",");
                String datePart = parts[0];
                String millisPart = parts[1];

                java.time.LocalDateTime dateTime = java.time.LocalDateTime.parse(
                    datePart.replace(" ", "T"), 
                    java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME
                );

                long millis = dateTime.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
                millis += Integer.parseInt(millisPart);

                logger.debug("Parsed timestamp: {} to {}", timestamp, millis);
                return millis;
            } else if (timestamp.matches("\\d{4}-\\d{2}-\\d{2}\\s\\d{2}:\\d{2}:\\d{2}")) {
                // Format: 2023-01-01 12:34:56
                java.time.LocalDateTime dateTime = java.time.LocalDateTime.parse(
                    timestamp.replace(" ", "T"), 
                    java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME
                );

                long millis = dateTime.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();

                logger.debug("Parsed timestamp: {} to {}", timestamp, millis);
                return millis;
            }

            // If we couldn't parse the timestamp, use the current time
            logger.warn("Could not parse timestamp: {}, using current time instead", timestamp);
            return System.currentTimeMillis();
        } catch (Exception e) {
            logger.error("Error parsing timestamp: {}", timestamp, e);
            return System.currentTimeMillis();
        }
    }

    /**
     * Get all log directory configurations.
     *
     * @return A list of all configurations
     */
    public List<LogDirectoryConfig> getAllConfigs() {
        return configRepository.findAll();
    }

    /**
     * Get a log directory configuration by ID.
     *
     * @param id The ID of the configuration to get
     * @return The configuration, or null if not found
     */
    public LogDirectoryConfig getConfigById(String id) {
        return configRepository.findById(id);
    }

    /**
     * Save a log directory configuration.
     *
     * @param config The configuration to save
     * @return The saved configuration
     */
    public LogDirectoryConfig saveConfig(LogDirectoryConfig config) {
        return configRepository.save(config);
    }

    /**
     * Delete a log directory configuration by ID.
     *
     * @param id The ID of the configuration to delete
     * @return true if the configuration was deleted, false otherwise
     */
    public boolean deleteConfig(String id) {
        return configRepository.deleteById(id);
    }

    /**
     * Manually trigger a scan of all directories.
     * This is useful for testing and for users who want to scan directories immediately.
     *
     * @return The number of directories scanned
     */
    public int manualScanAllDirectories() {
        logger.info("Manually triggering scan of all log directories");
        scanAllDirectories();
        return configRepository.count();
    }
}
