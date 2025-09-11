package io.github.ozkanpakdil.grepwise.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.github.ozkanpakdil.grepwise.model.ArchiveConfiguration;
import io.github.ozkanpakdil.grepwise.model.ArchiveMetadata;
import io.github.ozkanpakdil.grepwise.model.LogEntry;
import io.github.ozkanpakdil.grepwise.repository.ArchiveConfigurationRepository;
import io.github.ozkanpakdil.grepwise.repository.ArchiveMetadataRepository;
import jakarta.annotation.PostConstruct;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service for managing log archives.
 * This service provides methods for archiving logs, retrieving archives, and managing archive lifecycle.
 */
@Service
public class ArchiveService {
    private volatile ArchiveConfiguration cachedConfig;
    private static final Logger logger = LoggerFactory.getLogger(ArchiveService.class);
    private static final DateTimeFormatter FILENAME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final String METADATA_FILENAME = "metadata.json";
    private static final String LOGS_FILENAME = "logs.json";

    private final ArchiveConfigurationRepository archiveConfigurationRepository;
    private final ArchiveMetadataRepository archiveMetadataRepository;
    private final ObjectMapper objectMapper;

    @Autowired
    public ArchiveService(
            ArchiveConfigurationRepository archiveConfigurationRepository,
            ArchiveMetadataRepository archiveMetadataRepository) {
        this.archiveConfigurationRepository = archiveConfigurationRepository;
        this.archiveMetadataRepository = archiveMetadataRepository;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.findAndRegisterModules();
        logger.info("ArchiveService initialized");
    }

    /**
     * Initialize the archive service.
     */
    @PostConstruct
    public void init() throws IOException {
        // Create archive directory if it doesn't exist
        ArchiveConfiguration config = archiveConfigurationRepository.getDefaultConfiguration();
        cachedConfig = config;
        Path archivePath = Paths.get(config.getArchiveDirectory());
        if (!Files.exists(archivePath)) {
            Files.createDirectories(archivePath);
            logger.info("Created archive directory: {}", archivePath);
        }
    }

    /**
     * Archive logs.
     *
     * @param logs The logs to archive
     * @return The metadata for the created archive
     */
    public ArchiveMetadata archiveLogs(List<LogEntry> logs) throws IOException {
        if (logs == null || logs.isEmpty()) {
            logger.warn("No logs to archive");
            return null;
        }

        ArchiveConfiguration config = archiveConfigurationRepository.getDefaultConfiguration();

        // Generate archive filename
        String timestamp = LocalDateTime.now().format(FILENAME_FORMATTER);
        String filename = "logs_" + timestamp + ".zip";
        Path archivePath = Paths.get(config.getArchiveDirectory(), filename);

        // Collect metadata
        long startTimestamp = logs.stream()
                .mapToLong(LogEntry::timestamp)
                .min()
                .orElse(0);

        long endTimestamp = logs.stream()
                .mapToLong(LogEntry::timestamp)
                .max()
                .orElse(0);

        Set<String> sources = logs.stream()
                .map(LogEntry::source)
                .filter(s -> s != null && !s.isEmpty())
                .collect(Collectors.toSet());

        // Create archive metadata
        ArchiveMetadata metadata = new ArchiveMetadata();
        metadata.setFilename(filename);
        metadata.setStartTimestamp(startTimestamp);
        metadata.setEndTimestamp(endTimestamp);
        metadata.setLogCount(logs.size());
        metadata.setSources(sources);
        metadata.setCompressionType("zip");
        metadata.setCompressionLevel(config.getCompressionLevel());

        // Create archive file
        try (ZipArchiveOutputStream zipOut = new ZipArchiveOutputStream(
                new BufferedOutputStream(new FileOutputStream(archivePath.toFile())))) {

            // Set compression level
            zipOut.setLevel(config.getCompressionLevel());

            // Add metadata file
            ZipArchiveEntry metadataEntry = new ZipArchiveEntry(METADATA_FILENAME);
            zipOut.putArchiveEntry(metadataEntry);
            String metadataJson = objectMapper.writeValueAsString(metadata);
            zipOut.write(metadataJson.getBytes(StandardCharsets.UTF_8));
            zipOut.closeArchiveEntry();

            // Add logs file
            ZipArchiveEntry logsEntry = new ZipArchiveEntry(LOGS_FILENAME);
            zipOut.putArchiveEntry(logsEntry);

            // Write logs as JSON lines (one JSON object per line)
            for (LogEntry log : logs) {
                String logJson = objectMapper.writeValueAsString(log);
                byte[] bytes = (logJson + "\n").getBytes(StandardCharsets.UTF_8);
                zipOut.write(bytes);
            }
            zipOut.closeArchiveEntry();
        }

        // Update metadata with file size
        long fileSize = Files.size(archivePath);
        metadata.setSizeBytes(fileSize);

        // Save metadata
        ArchiveMetadata savedMetadata = archiveMetadataRepository.save(metadata);
        logger.info("Archived {} logs to {}, size: {} bytes", logs.size(), archivePath, fileSize);

        return savedMetadata;
    }

    /**
     * Extract logs from an archive.
     *
     * @param archiveId The ID of the archive to extract logs from
     * @return The extracted logs
     */
    public List<LogEntry> extractLogs(String archiveId) throws IOException {
        ArchiveMetadata metadata = archiveMetadataRepository.findById(archiveId);
        if (metadata == null) {
            logger.warn("Archive not found: {}", archiveId);
            return List.of();
        }

        ArchiveConfiguration config = archiveConfigurationRepository.getDefaultConfiguration();
        Path archivePath = Paths.get(config.getArchiveDirectory(), metadata.getFilename());

        if (!Files.exists(archivePath)) {
            logger.warn("Archive file not found: {}", archivePath);
            archiveMetadataRepository.markUnavailable(archiveId);
            return List.of();
        }

        List<LogEntry> logs = new ArrayList<>();

        // Read the logs.jsonl entry from the ZIP archive
        try (org.apache.commons.compress.archivers.zip.ZipFile zipFile = new org.apache.commons.compress.archivers.zip.ZipFile(archivePath.toFile(), StandardCharsets.UTF_8.name())) {
            ZipArchiveEntry logsEntry = zipFile.getEntry(LOGS_FILENAME);
            if (logsEntry == null) {
                logger.warn("Logs entry '{}' not found in archive: {}", LOGS_FILENAME, archivePath);
                return List.of();
            }

            try (InputStream is = zipFile.getInputStream(logsEntry);
                 BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.trim().isEmpty()) {
                        LogEntry log = objectMapper.readValue(line, LogEntry.class);
                        logs.add(log);
                    }
                }
            }
        } catch (IOException e) {
            logger.error("Error extracting logs from archive: {}", archivePath, e);
            throw e;
        }

        logger.info("Extracted {} logs from archive: {}", logs.size(), archivePath);
        return logs;
    }

    /**
     * Get all archive metadata.
     *
     * @return A list of all archive metadata
     */
    public List<ArchiveMetadata> getAllArchiveMetadata() {
        return archiveMetadataRepository.findAllAvailableSortedByCreationTime();
    }

    /**
     * Get archive metadata by ID.
     *
     * @param id The ID of the archive metadata to get
     * @return The archive metadata, or null if not found
     */
    public ArchiveMetadata getArchiveMetadataById(String id) {
        return archiveMetadataRepository.findById(id);
    }

    /**
     * Get archive metadata by source.
     *
     * @param source The source to filter by
     * @return A list of archive metadata for the specified source
     */
    public List<ArchiveMetadata> getArchiveMetadataBySource(String source) {
        return archiveMetadataRepository.findBySource(source);
    }

    /**
     * Get archive metadata by time range.
     *
     * @param startTimestamp The start timestamp
     * @param endTimestamp   The end timestamp
     * @return A list of archive metadata that overlap with the specified time range
     */
    public List<ArchiveMetadata> getArchiveMetadataByTimeRange(long startTimestamp, long endTimestamp) {
        return archiveMetadataRepository.findByTimeRange(startTimestamp, endTimestamp);
    }

    /**
     * Delete an archive by ID.
     *
     * @param id The ID of the archive to delete
     * @return true if the archive was deleted, false otherwise
     */
    public boolean deleteArchive(String id) throws IOException {
        ArchiveMetadata metadata = archiveMetadataRepository.findById(id);
        if (metadata == null) {
            logger.warn("Archive not found: {}", id);
            return false;
        }

        ArchiveConfiguration config = archiveConfigurationRepository.getDefaultConfiguration();
        Path archivePath = Paths.get(config.getArchiveDirectory(), metadata.getFilename());

        // Delete archive file if it exists
        if (Files.exists(archivePath)) {
            Files.delete(archivePath);
            logger.info("Deleted archive file: {}", archivePath);
        }

        // Delete metadata
        boolean deleted = archiveMetadataRepository.deleteById(id);
        if (deleted) {
            logger.info("Deleted archive metadata: {}", id);
        }

        return deleted;
    }

    /**
     * Get the archive configuration.
     *
     * @return The archive configuration
     */
    public ArchiveConfiguration getArchiveConfiguration() {
        // Use cached configuration to avoid unnecessary repository calls in tests
        return cachedConfig != null ? cachedConfig : archiveConfigurationRepository.getDefaultConfiguration();
    }

    /**
     * Update the archive configuration.
     *
     * @param configuration The updated configuration
     * @return The updated configuration
     */
    public ArchiveConfiguration updateArchiveConfiguration(ArchiveConfiguration configuration) {
        ArchiveConfiguration saved = archiveConfigurationRepository.save(configuration);
        cachedConfig = saved;
        return saved;
    }

    /**
     * Scheduled task to clean up old archives based on retention policy.
     */
    @Scheduled(cron = "0 0 2 * * ?") // Run at 2:00 AM every day
    public void cleanupOldArchives() throws IOException {
        logger.info("Starting scheduled cleanup of old archives");

        ArchiveConfiguration config = archiveConfigurationRepository.getDefaultConfiguration();
        int retentionDays = config.getArchiveRetentionDays();

        // Calculate cutoff date
        Instant cutoffDate = Instant.now().minusSeconds((long) retentionDays * 24 * 60 * 60);
        logger.info("Cleaning up archives older than: {}", cutoffDate);

        // Find archives older than cutoff date
        List<ArchiveMetadata> oldArchives = archiveMetadataRepository.findOlderThan(cutoffDate);
        logger.info("Found {} archives to clean up", oldArchives.size());

        // Delete old archives
        int deletedCount = 0;
        for (ArchiveMetadata metadata : oldArchives) {
            try {
                if (deleteArchive(metadata.getId())) {
                    deletedCount++;
                }
            } catch (IOException e) {
                logger.error("Error deleting archive: {}", metadata.getId(), e);
            }
        }

        logger.info("Cleaned up {} old archives", deletedCount);
    }

    /**
     * Archive logs before deletion.
     *
     * @param logs The logs to archive
     * @return true if the logs were archived successfully, false otherwise
     */
    public boolean archiveLogsBeforeDeletion(List<LogEntry> logs) {
        try {
            ArchiveConfiguration config = archiveConfigurationRepository.getDefaultConfiguration();
            if (!config.isAutoArchiveEnabled() || logs.isEmpty()) {
                return true; // Skip archiving if disabled or no logs
            }

            ArchiveMetadata metadata = archiveLogs(logs);
            return metadata != null;
        } catch (Exception e) {
            logger.error("Error archiving logs before deletion", e);
            return false;
        }
    }
}