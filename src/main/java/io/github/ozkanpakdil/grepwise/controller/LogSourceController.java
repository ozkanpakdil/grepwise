package io.github.ozkanpakdil.grepwise.controller;

import io.github.ozkanpakdil.grepwise.model.LogSourceConfig;
import io.github.ozkanpakdil.grepwise.service.LogSourceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller for managing log sources.
 * Provides endpoints for creating, reading, updating, and deleting log sources of different types.
 */
@RestController
@RequestMapping("/api/sources")
public class LogSourceController {
    private static final Logger logger = LoggerFactory.getLogger(LogSourceController.class);
    
    private final LogSourceService logSourceService;
    
    public LogSourceController(LogSourceService logSourceService) {
        this.logSourceService = logSourceService;
        logger.info("LogSourceController initialized");
    }
    
    /**
     * Get all log sources.
     * 
     * @return A list of all log sources
     */
    @GetMapping
    public ResponseEntity<List<LogSourceConfig>> getAllSources() {
        logger.debug("Getting all log sources");
        List<LogSourceConfig> sources = logSourceService.getAllSources();
        return ResponseEntity.ok(sources);
    }
    
    /**
     * Get a log source by ID.
     * 
     * @param id The ID of the log source
     * @return The log source, or 404 if not found
     */
    @GetMapping("/{id}")
    public ResponseEntity<LogSourceConfig> getSourceById(@PathVariable String id) {
        logger.debug("Getting log source by ID: {}", id);
        LogSourceConfig source = logSourceService.getSourceById(id);
        
        if (source == null) {
            logger.warn("Log source not found: {}", id);
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(source);
    }
    
    /**
     * Get all log sources of a specific type.
     * 
     * @param type The type of log source (FILE, SYSLOG, HTTP)
     * @return A list of log sources of the specified type
     */
    @GetMapping("/type/{type}")
    public ResponseEntity<List<LogSourceConfig>> getSourcesByType(@PathVariable String type) {
        logger.debug("Getting log sources by type: {}", type);
        
        try {
            LogSourceConfig.SourceType sourceType = LogSourceConfig.SourceType.valueOf(type.toUpperCase());
            List<LogSourceConfig> sources = logSourceService.getSourcesByType(sourceType);
            return ResponseEntity.ok(sources);
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid log source type: {}", type);
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Create a new log source.
     * 
     * @param config The log source configuration
     * @return The created log source
     */
    @PostMapping
    public ResponseEntity<LogSourceConfig> createSource(@RequestBody LogSourceConfig config) {
        logger.debug("Creating log source: {}", config);
        
        try {
            LogSourceConfig createdSource = logSourceService.createSource(config);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdSource);
        } catch (Exception e) {
            logger.error("Error creating log source", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Update an existing log source.
     * 
     * @param id The ID of the log source to update
     * @param config The updated log source configuration
     * @return The updated log source, or 404 if not found
     */
    @PutMapping("/{id}")
    public ResponseEntity<LogSourceConfig> updateSource(@PathVariable String id, @RequestBody LogSourceConfig config) {
        logger.debug("Updating log source: {}", id);
        
        try {
            LogSourceConfig updatedSource = logSourceService.updateSource(id, config);
            
            if (updatedSource == null) {
                logger.warn("Log source not found: {}", id);
                return ResponseEntity.notFound().build();
            }
            
            return ResponseEntity.ok(updatedSource);
        } catch (Exception e) {
            logger.error("Error updating log source: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Delete a log source.
     * 
     * @param id The ID of the log source to delete
     * @return 204 No Content if successful, 404 if not found
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSource(@PathVariable String id) {
        logger.debug("Deleting log source: {}", id);
        
        boolean deleted = logSourceService.deleteSource(id);
        
        if (!deleted) {
            logger.warn("Log source not found: {}", id);
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.noContent().build();
    }
    
    /**
     * Start a log source.
     * 
     * @param id The ID of the log source to start
     * @return A response indicating success or failure
     */
    @PostMapping("/{id}/start")
    public ResponseEntity<Map<String, Object>> startSource(@PathVariable String id) {
        logger.debug("Starting log source: {}", id);
        
        LogSourceConfig source = logSourceService.getSourceById(id);
        
        if (source == null) {
            logger.warn("Log source not found: {}", id);
            return ResponseEntity.notFound().build();
        }
        
        boolean started = logSourceService.startSource(source);
        
        Map<String, Object> response = new HashMap<>();
        response.put("id", id);
        response.put("success", started);
        
        if (started) {
            response.put("message", "Log source started successfully");
            logger.info("Log source started: {}", id);
            return ResponseEntity.ok(response);
        } else {
            response.put("message", "Failed to start log source");
            logger.warn("Failed to start log source: {}", id);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * Stop a log source.
     * 
     * @param id The ID of the log source to stop
     * @return A response indicating success or failure
     */
    @PostMapping("/{id}/stop")
    public ResponseEntity<Map<String, Object>> stopSource(@PathVariable String id) {
        logger.debug("Stopping log source: {}", id);
        
        LogSourceConfig source = logSourceService.getSourceById(id);
        
        if (source == null) {
            logger.warn("Log source not found: {}", id);
            return ResponseEntity.notFound().build();
        }
        
        boolean stopped = logSourceService.stopSource(source);
        
        Map<String, Object> response = new HashMap<>();
        response.put("id", id);
        response.put("success", stopped);
        
        if (stopped) {
            response.put("message", "Log source stopped successfully");
            logger.info("Log source stopped: {}", id);
            return ResponseEntity.ok(response);
        } else {
            response.put("message", "Failed to stop log source");
            logger.warn("Failed to stop log source: {}", id);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * Get statistics about log sources.
     * 
     * @return Statistics about log sources
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getSourceStats() {
        logger.debug("Getting log source statistics");
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalSources", logSourceService.getAllSources().size());
        stats.put("activeSources", logSourceService.getTotalActiveSourceCount());
        stats.put("sourcesByType", logSourceService.getActiveSourceCounts());
        
        return ResponseEntity.ok(stats);
    }
}