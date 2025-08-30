package io.github.ozkanpakdil.grepwise.controller;

import io.github.ozkanpakdil.grepwise.service.ConfigurationBackupService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * REST controller for configuration backup and restore operations.
 * This controller provides endpoints for exporting and importing application configurations.
 */
@RestController
@RequestMapping("/api/config/backup")
@CrossOrigin(origins = "*") // Allow requests from any origin for development
public class ConfigurationBackupController {

    private final ConfigurationBackupService configurationBackupService;

    public ConfigurationBackupController(ConfigurationBackupService configurationBackupService) {
        this.configurationBackupService = configurationBackupService;
    }

    /**
     * Export all configurations as a JSON file.
     *
     * @return A JSON file containing all configurations
     */
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportConfigurations() {
        String json = configurationBackupService.exportConfigurations();

        // Generate a filename with current date and time
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String filename = "grepwise_config_backup_" + timestamp + ".json";

        // Set headers for file download
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setContentDispositionFormData("attachment", filename);

        return ResponseEntity.ok()
                .headers(headers)
                .body(json.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Import configurations from a JSON file.
     *
     * @param file      The JSON file containing configurations
     * @param overwrite Whether to overwrite existing configurations (default: false)
     * @return A summary of the import operation
     */
    @PostMapping("/import")
    public ResponseEntity<Map<String, Object>> importConfigurations(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "overwrite", defaultValue = "false") boolean overwrite) {

        try {
            String json = new String(file.getBytes(), StandardCharsets.UTF_8);
            Map<String, Object> summary = configurationBackupService.importConfigurations(json, overwrite);
            return ResponseEntity.ok(summary);
        } catch (IOException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}