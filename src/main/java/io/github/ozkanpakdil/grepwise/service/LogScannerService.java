package io.github.ozkanpakdil.grepwise.service;

import io.github.ozkanpakdil.grepwise.model.LogDirectoryConfig;
import io.github.ozkanpakdil.grepwise.model.LogEntry;
import io.github.ozkanpakdil.grepwise.repository.LogDirectoryConfigRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.util.Map;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * Service for scanning log files in specified directories.
 */
@Service
public class LogScannerService {
    private static final Logger logger = LoggerFactory.getLogger(LogScannerService.class);

    // No log patterns - we'll index the entire line without parsing

    private final LogDirectoryConfigRepository configRepository;
    private final LuceneService luceneService;
    private final LogBufferService logBufferService;
    private final NginxLogParser nginxLogParser;
    private final ApacheLogParser apacheLogParser;
    private final LogPatternRecognitionService patternRecognitionService;
    
    @Value("${grepwise.log-scanner.use-buffer:true}")
    private boolean useBuffer;
    
    @Value("${grepwise.log-scanner.analyze-patterns:true}")
    private boolean analyzePatterns;

    public LogScannerService(LogDirectoryConfigRepository configRepository, 
                            LuceneService luceneService,
                            LogBufferService logBufferService,
                            NginxLogParser nginxLogParser,
                            ApacheLogParser apacheLogParser,
                            LogPatternRecognitionService patternRecognitionService, RealTimeUpdateService realTimeUpdateService) {
        this.configRepository = configRepository;
        this.luceneService = luceneService;
        this.logBufferService = logBufferService;
        this.nginxLogParser = nginxLogParser;
        this.apacheLogParser = apacheLogParser;
        this.patternRecognitionService = patternRecognitionService;
        logger.info("LogScannerService initialized");
    }

    /**
     * Scan all configured directories for log files. This method is scheduled to run periodically.
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

        Path dir = Paths.get(directoryPath);
        if (!Files.exists(dir) || !Files.isDirectory(dir)) {
            logger.error("Directory does not exist or is not a directory: {}", directoryPath);
            return 0;
        }

        List<Path> logFiles;
        try (Stream<Path> stream = Files.list(dir)) {
            logFiles = stream
                    .filter(Files::isRegularFile)
                    .toList();

            logger.info("Found {} log files matching pattern in directory: {}", logFiles.size(), directoryPath);
        } catch (IOException e) {
            logger.error("Error listing files in directory: {}", directoryPath, e);
            return 0;
        }

        int totalProcessed = 0;
        List<LogEntry> processedEntries = new ArrayList<>();
        
        for (Path logFile : logFiles) {
            try {
                int processed = processLogFile(logFile.toFile());
                totalProcessed += processed;
                logger.info("Processed {} log entries from file: {}", processed, logFile);
                
                // If pattern analysis is enabled, collect log entries for analysis
                if (analyzePatterns && processed > 0) {
                    try {
                        // Get the most recent log entries from this file
                        List<LogEntry> entries = luceneService.findBySource(logFile.getFileName().toString());
                        processedEntries.addAll(entries);
                    } catch (IOException e) {
                        logger.error("Error retrieving log entries for pattern analysis: {}", logFile, e);
                    }
                }
            } catch (Exception e) {
                logger.error("Error processing log file: {}", logFile, e);
            }
        }
        
        // Analyze patterns in the processed log entries
        if (analyzePatterns && !processedEntries.isEmpty()) {
            analyzeLogPatterns(processedEntries);
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
        
        // If buffering is disabled, use the original approach
        if (!useBuffer) {
            return processLogFileDirectIndexing(file);
        }
        
        // Use buffering approach
        int processedCount = 0;
        
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            int lineCount = 0;

            while ((line = reader.readLine()) != null) {
                lineCount++;
                // Create a log entry for each line
                LogEntry entry = parseLogLine(line, file.getName());
                
                // Add to buffer instead of collecting in memory
                logBufferService.addToBuffer(entry);
                processedCount++;

                logger.trace("Processed line {}: {}", lineCount,
                        line.substring(0, Math.min(line.length(), 100)));
            }

            logger.debug("Processed {} lines in file using buffer", lineCount);
            return processedCount;
        } catch (IOException e) {
            logger.error("Error reading log file: {}", file, e);
            return 0;
        }
    }
    
    /**
     * Process a log file using direct indexing (no buffering).
     * This is the original implementation before buffering was added.
     *
     * @param file The log file to process
     * @return The number of log entries processed
     */
    private int processLogFileDirectIndexing(File file) {
        logger.debug("Processing log file with direct indexing: {}", file.getAbsolutePath());
        List<LogEntry> logEntries = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            int lineCount = 0;

            while ((line = reader.readLine()) != null) {
                lineCount++;
                // Create a log entry for each line
                LogEntry entry = parseLogLine(line, file.getName());
                logEntries.add(entry);

                logger.trace("Processed line {}: {}", lineCount,
                        line.substring(0, Math.min(line.length(), 100)));
            }

            logger.debug("Processed {} lines in file", lineCount);
        } catch (IOException e) {
            logger.error("Error reading log file: {}", file, e);
            return 0;
        }

        // Index all log entries in Lucene
        try {
            int savedCount = luceneService.indexLogEntries(logEntries);
            logger.info("Indexed {} log entries from file: {}", savedCount, file.getName());
            return savedCount;
        } catch (IOException e) {
            logger.error("Error indexing log entries: {}", e.getMessage(), e);
            return 0;
        }
    }

    /**
     * Parse a log line and extract a log entry.
     *
     * @param line The log line to parse
     * @param source The source of the log (filename)
     * @return A LogEntry with the entire line as the message
     */
    private LogEntry parseLogLine(String line, String source) {
        // First, check if this is an Nginx log format
        if (nginxLogParser.isNginxLogFormat(line)) {
            logger.debug("Detected Nginx log format: {}", line);
            LogEntry nginxLogEntry = nginxLogParser.parseNginxLogLine(line, source);
            if (nginxLogEntry != null) {
                return nginxLogEntry;
            }
        }
        
        // Next, check if this is an Apache log format
        if (apacheLogParser.isApacheLogFormat(line)) {
            logger.debug("Detected Apache log format: {}", line);
            LogEntry apacheLogEntry = apacheLogParser.parseApacheLogLine(line, source);
            if (apacheLogEntry != null) {
                return apacheLogEntry;
            }
        }
        
        // If not a recognized log format or parsing failed, fall back to generic parsing
        
        // Extract log level if possible
        String LOGLEVEL = switch (line) {
            case String s when s.contains("ERROR") -> "ERROR";
            case String s when s.contains("WARN") -> "WARN";
            case String s when s.contains("INFO") -> "INFO";
            case String s when s.contains("DEBUG") -> "DEBUG";
            case String s when s.contains("TRACE") -> "TRACE";
            case String s when s.contains("FATAL") -> "FATAL";
            case String s when s.contains("SEVERE") -> "SEVERE";
            case String s when s.contains("WARNING") -> "WARNING";
            case String s when s.contains("NOTICE") -> "NOTICE";
            case String s when s.contains("ALERT") -> "ALERT";
            case String s when s.contains("CRITICAL") -> "CRITICAL";
            case String s when s.contains("EMERGENCY") -> "EMERGENCY";
            default -> "UNKNOWN";
        };

        // Try to extract timestamp if possible
        Long RECORDTIME = DateTimeRegexPatterns.extractDateTimeToTimestamp(DateTimeRegexPatterns.extractFirstDateTime(line));

        // Create a log entry with the entire line as the message and raw content
        return new LogEntry(
                UUID.randomUUID().toString(),
                System.currentTimeMillis(), // Entry time (when the log was scanned)
                RECORDTIME, // Record time (extracted if possible)
                LOGLEVEL, // Level (extracted if possible)
                line, // Message is the whole line
                source, // Source is the filename
                new HashMap<>(), // No metadata
                line // Raw content is the whole line
        );
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
     * Manually trigger a scan of all directories. This is useful for testing and for users who want to scan directories
     * immediately.
     *
     * @return The number of directories scanned
     */
    public int manualScanAllDirectories() {
        logger.info("Manually triggering scan of all log directories");
        scanAllDirectories();
        return configRepository.count();
    }
    
    /**
     * Analyze patterns in a batch of log entries.
     * This method uses the LogPatternRecognitionService to identify common patterns
     * in the log entries and logs the results.
     *
     * @param logEntries The log entries to analyze
     */
    private void analyzeLogPatterns(List<LogEntry> logEntries) {
        if (!analyzePatterns || logEntries.isEmpty()) {
            return;
        }
        
        try {
            logger.info("Analyzing patterns in {} log entries", logEntries.size());
            
            // Get the most common patterns (top 10)
            Map<String, Integer> commonPatterns = patternRecognitionService.getMostCommonPatterns(null, null, 10);
            
            // Log the results
            logger.info("Found {} distinct log patterns", commonPatterns.size());
            for (Map.Entry<String, Integer> entry : commonPatterns.entrySet()) {
                logger.info("Pattern: '{}' - Count: {}", entry.getKey(), entry.getValue());
            }
        } catch (Exception e) {
            logger.error("Error analyzing log patterns", e);
        }
    }
}
