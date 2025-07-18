package io.github.ozkanpakdil.grepwise.controller;

import io.github.ozkanpakdil.grepwise.model.ArchiveConfiguration;
import io.github.ozkanpakdil.grepwise.model.ArchiveMetadata;
import io.github.ozkanpakdil.grepwise.model.LogEntry;
import io.github.ozkanpakdil.grepwise.service.ArchiveService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Controller for managing log archives.
 */
@RestController
@RequestMapping("/api/archives")
public class ArchiveController {
    private static final Logger logger = LoggerFactory.getLogger(ArchiveController.class);
    
    private final ArchiveService archiveService;
    
    @Autowired
    public ArchiveController(ArchiveService archiveService) {
        this.archiveService = archiveService;
        logger.info("ArchiveController initialized");
    }
    
    /**
     * Get all archive metadata.
     *
     * @return A list of all archive metadata
     */
    @GetMapping
    public ResponseEntity<List<ArchiveMetadata>> getAllArchiveMetadata() {
        logger.debug("Getting all archive metadata");
        return ResponseEntity.ok(archiveService.getAllArchiveMetadata());
    }
    
    /**
     * Get archive metadata by ID.
     *
     * @param id The ID of the archive metadata to get
     * @return The archive metadata
     */
    @GetMapping("/{id}")
    public ResponseEntity<ArchiveMetadata> getArchiveMetadataById(@PathVariable String id) {
        logger.debug("Getting archive metadata by ID: {}", id);
        ArchiveMetadata metadata = archiveService.getArchiveMetadataById(id);
        if (metadata == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(metadata);
    }
    
    /**
     * Get archive metadata by source.
     *
     * @param source The source to filter by
     * @return A list of archive metadata for the specified source
     */
    @GetMapping("/source/{source}")
    public ResponseEntity<List<ArchiveMetadata>> getArchiveMetadataBySource(@PathVariable String source) {
        logger.debug("Getting archive metadata by source: {}", source);
        return ResponseEntity.ok(archiveService.getArchiveMetadataBySource(source));
    }
    
    /**
     * Get archive metadata by time range.
     *
     * @param startTimestamp The start timestamp
     * @param endTimestamp The end timestamp
     * @return A list of archive metadata that overlap with the specified time range
     */
    @GetMapping("/timerange")
    public ResponseEntity<List<ArchiveMetadata>> getArchiveMetadataByTimeRange(
            @RequestParam long startTimestamp,
            @RequestParam long endTimestamp) {
        logger.debug("Getting archive metadata by time range: {} - {}", startTimestamp, endTimestamp);
        return ResponseEntity.ok(archiveService.getArchiveMetadataByTimeRange(startTimestamp, endTimestamp));
    }
    
    /**
     * Extract logs from an archive.
     *
     * @param id The ID of the archive to extract logs from
     * @return The extracted logs
     */
    @GetMapping("/{id}/logs")
    public ResponseEntity<List<LogEntry>> extractLogs(@PathVariable String id) {
        logger.debug("Extracting logs from archive: {}", id);
        try {
            List<LogEntry> logs = archiveService.extractLogs(id);
            if (logs.isEmpty()) {
                return ResponseEntity.noContent().build();
            }
            return ResponseEntity.ok(logs);
        } catch (IOException e) {
            logger.error("Error extracting logs from archive: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Delete an archive.
     *
     * @param id The ID of the archive to delete
     * @return A response indicating success or failure
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteArchive(@PathVariable String id) {
        logger.debug("Deleting archive: {}", id);
        try {
            boolean deleted = archiveService.deleteArchive(id);
            if (deleted) {
                return ResponseEntity.ok(Map.of("success", true, "message", "Archive deleted successfully"));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("success", false, "message", "Archive not found"));
            }
        } catch (IOException e) {
            logger.error("Error deleting archive: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Error deleting archive: " + e.getMessage()));
        }
    }
    
    /**
     * Get the archive configuration.
     *
     * @return The archive configuration
     */
    @GetMapping("/config")
    public ResponseEntity<ArchiveConfiguration> getArchiveConfiguration() {
        logger.debug("Getting archive configuration");
        return ResponseEntity.ok(archiveService.getArchiveConfiguration());
    }
    
    /**
     * Update the archive configuration.
     *
     * @param configuration The updated configuration
     * @return The updated configuration
     */
    @PutMapping("/config")
    public ResponseEntity<ArchiveConfiguration> updateArchiveConfiguration(
            @RequestBody ArchiveConfiguration configuration) {
        logger.debug("Updating archive configuration: {}", configuration);
        return ResponseEntity.ok(archiveService.updateArchiveConfiguration(configuration));
    }
    
    /**
     * Manually create an archive with logs matching the specified criteria.
     *
     * @param startTimestamp The start timestamp
     * @param endTimestamp The end timestamp
     * @param source The source to filter by (optional)
     * @return The created archive metadata
     */
    @PostMapping("/create")
    public ResponseEntity<ArchiveMetadata> createArchive(
            @RequestParam long startTimestamp,
            @RequestParam long endTimestamp,
            @RequestParam(required = false) String source) {
        logger.debug("Creating archive for logs from {} to {}, source: {}", startTimestamp, endTimestamp, source);
        try {
            // This would require adding a method to LuceneService to get logs by time range and source
            // For now, we'll return a not implemented response
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
        } catch (Exception e) {
            logger.error("Error creating archive", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}