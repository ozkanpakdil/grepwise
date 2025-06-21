package io.github.ozkanpakdil.grepwise.controller;

import io.github.ozkanpakdil.grepwise.model.LogDirectoryConfig;
import io.github.ozkanpakdil.grepwise.service.LogScannerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for managing log directory configurations.
 */
@RestController
@RequestMapping("/api/config/log-directories")
@CrossOrigin(origins = "*") // Allow requests from any origin for development
public class LogDirectoryConfigController {

    private final LogScannerService logScannerService;

    public LogDirectoryConfigController(LogScannerService logScannerService) {
        this.logScannerService = logScannerService;
    }

    /**
     * Get all log directory configurations.
     *
     * @return A list of all configurations
     */
    @GetMapping
    public ResponseEntity<List<LogDirectoryConfig>> getAllConfigs() {
        return ResponseEntity.ok(logScannerService.getAllConfigs());
    }

    /**
     * Get a log directory configuration by ID.
     *
     * @param id The ID of the configuration to get
     * @return The configuration
     */
    @GetMapping("/{id}")
    public ResponseEntity<LogDirectoryConfig> getConfigById(@PathVariable String id) {
        LogDirectoryConfig config = logScannerService.getConfigById(id);
        if (config == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(config);
    }

    /**
     * Create a new log directory configuration.
     *
     * @param config The configuration to create
     * @return The created configuration
     */
    @PostMapping
    public ResponseEntity<LogDirectoryConfig> createConfig(@RequestBody LogDirectoryConfig config) {
        return ResponseEntity.ok(logScannerService.saveConfig(config));
    }

    /**
     * Update an existing log directory configuration.
     *
     * @param id The ID of the configuration to update
     * @param config The updated configuration
     * @return The updated configuration
     */
    @PutMapping("/{id}")
    public ResponseEntity<LogDirectoryConfig> updateConfig(@PathVariable String id, @RequestBody LogDirectoryConfig config) {
        LogDirectoryConfig existingConfig = logScannerService.getConfigById(id);
        if (existingConfig == null) {
            return ResponseEntity.notFound().build();
        }

        config.setId(id);
        return ResponseEntity.ok(logScannerService.saveConfig(config));
    }

    /**
     * Delete a log directory configuration.
     *
     * @param id The ID of the configuration to delete
     * @return No content if successful
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteConfig(@PathVariable String id) {
        if (logScannerService.deleteConfig(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * Trigger a scan of a specific directory.
     *
     * @param id The ID of the configuration to scan
     * @return The number of log entries processed
     */
    @PostMapping("/{id}/scan")
    public ResponseEntity<Integer> scanDirectory(@PathVariable String id) {
        LogDirectoryConfig config = logScannerService.getConfigById(id);
        if (config == null) {
            return ResponseEntity.notFound().build();
        }

        int processed = logScannerService.scanDirectory(config);
        return ResponseEntity.ok(processed);
    }

    /**
     * Trigger a scan of all directories.
     *
     * @return The number of directories scanned
     */
    @PostMapping("/scan-all")
    public ResponseEntity<Integer> scanAllDirectories() {
        int scanned = logScannerService.manualScanAllDirectories();
        return ResponseEntity.ok(scanned);
    }
}
