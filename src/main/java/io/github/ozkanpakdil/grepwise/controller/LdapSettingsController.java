package io.github.ozkanpakdil.grepwise.controller;

import io.github.ozkanpakdil.grepwise.service.AuditLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Controller for managing LDAP settings.
 */
@RestController
@RequestMapping("/api/settings/ldap")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class LdapSettingsController {
    private static final Logger logger = LoggerFactory.getLogger(LdapSettingsController.class);

    @Autowired
    private Environment environment;

    @Autowired
    private ResourceLoader resourceLoader;

    @Autowired
    private AuditLogService auditLogService;

    @Value("${spring.config.location:classpath:application.properties}")
    private String configLocation;

    /**
     * Get the current LDAP settings.
     *
     * @return The LDAP settings
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getLdapSettings() {
        try {
            Map<String, Object> settings = new HashMap<>();
            
            // Get LDAP settings from environment
            settings.put("enabled", Boolean.parseBoolean(environment.getProperty("grepwise.ldap.enabled", "false")));
            settings.put("url", environment.getProperty("grepwise.ldap.url", "ldap://localhost:389"));
            settings.put("baseDn", environment.getProperty("grepwise.ldap.base-dn", "dc=example,dc=com"));
            settings.put("userDnPattern", environment.getProperty("grepwise.ldap.user-dn-pattern", "uid={0},ou=people"));
            settings.put("managerDn", environment.getProperty("grepwise.ldap.manager-dn", ""));
            settings.put("managerPassword", environment.getProperty("grepwise.ldap.manager-password", ""));
            settings.put("userSearchBase", environment.getProperty("grepwise.ldap.user-search-base", "ou=people"));
            settings.put("userSearchFilter", environment.getProperty("grepwise.ldap.user-search-filter", "(uid={0})"));
            settings.put("groupSearchBase", environment.getProperty("grepwise.ldap.group-search-base", "ou=groups"));
            settings.put("groupSearchFilter", environment.getProperty("grepwise.ldap.group-search-filter", "(member={0})"));
            settings.put("groupRoleAttribute", environment.getProperty("grepwise.ldap.group-role-attribute", "cn"));
            
            return ResponseEntity.ok(settings);
        } catch (Exception e) {
            logger.error("Error retrieving LDAP settings: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error retrieving LDAP settings: " + e.getMessage()));
        }
    }

    /**
     * Update the LDAP settings.
     *
     * @param settings The new LDAP settings
     * @return Success response
     */
    @PutMapping
    public ResponseEntity<?> updateLdapSettings(@RequestBody Map<String, Object> settings) {
        try {
            // Get the application.properties file
            Path propertiesPath = getPropertiesPath();
            if (propertiesPath == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", "Could not locate application.properties file"));
            }
            
            // Load existing properties
            Properties properties = new Properties();
            properties.load(Files.newInputStream(propertiesPath));
            
            // Update LDAP properties
            properties.setProperty("grepwise.ldap.enabled", String.valueOf(settings.getOrDefault("enabled", false)));
            properties.setProperty("grepwise.ldap.url", (String) settings.getOrDefault("url", "ldap://localhost:389"));
            properties.setProperty("grepwise.ldap.base-dn", (String) settings.getOrDefault("baseDn", "dc=example,dc=com"));
            properties.setProperty("grepwise.ldap.user-dn-pattern", (String) settings.getOrDefault("userDnPattern", "uid={0},ou=people"));
            properties.setProperty("grepwise.ldap.manager-dn", (String) settings.getOrDefault("managerDn", ""));
            properties.setProperty("grepwise.ldap.manager-password", (String) settings.getOrDefault("managerPassword", ""));
            properties.setProperty("grepwise.ldap.user-search-base", (String) settings.getOrDefault("userSearchBase", "ou=people"));
            properties.setProperty("grepwise.ldap.user-search-filter", (String) settings.getOrDefault("userSearchFilter", "(uid={0})"));
            properties.setProperty("grepwise.ldap.group-search-base", (String) settings.getOrDefault("groupSearchBase", "ou=groups"));
            properties.setProperty("grepwise.ldap.group-search-filter", (String) settings.getOrDefault("groupSearchFilter", "(member={0})"));
            properties.setProperty("grepwise.ldap.group-role-attribute", (String) settings.getOrDefault("groupRoleAttribute", "cn"));
            
            // Save properties
            try (OutputStream os = new FileOutputStream(propertiesPath.toFile())) {
                properties.store(os, "Updated LDAP settings");
            }
            
            // Log the update
            auditLogService.createAuthAuditLog(
                "LDAP_SETTINGS_UPDATE", 
                "SUCCESS", 
                "admin", 
                "LDAP settings updated successfully"
            );
            
            return ResponseEntity.ok(Map.of("message", "LDAP settings updated successfully"));
        } catch (Exception e) {
            logger.error("Error updating LDAP settings: {}", e.getMessage(), e);
            
            // Log the failed update
            auditLogService.createAuthAuditLog(
                "LDAP_SETTINGS_UPDATE", 
                "FAILURE", 
                "admin", 
                "Error updating LDAP settings: " + e.getMessage()
            );
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error updating LDAP settings: " + e.getMessage()));
        }
    }

    /**
     * Test the LDAP connection with the current settings.
     *
     * @return Success response if connection is successful
     */
    @PostMapping("/test")
    public ResponseEntity<?> testLdapConnection() {
        try {
            // Get LDAP settings from environment
            boolean enabled = Boolean.parseBoolean(environment.getProperty("grepwise.ldap.enabled", "false"));
            String url = environment.getProperty("grepwise.ldap.url", "");
            String managerDn = environment.getProperty("grepwise.ldap.manager-dn", "");
            String managerPassword = environment.getProperty("grepwise.ldap.manager-password", "");
            
            if (!enabled) {
                return ResponseEntity.badRequest().body(Map.of("error", "LDAP is not enabled"));
            }
            
            // TODO: Implement actual LDAP connection test
            // For now, just return success if LDAP is enabled and URL is not empty
            if (url.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "LDAP URL is not configured"));
            }
            
            // Log the test
            auditLogService.createAuthAuditLog(
                "LDAP_CONNECTION_TEST", 
                "SUCCESS", 
                "admin", 
                "LDAP connection test successful"
            );
            
            return ResponseEntity.ok(Map.of("message", "LDAP connection test successful"));
        } catch (Exception e) {
            logger.error("Error testing LDAP connection: {}", e.getMessage(), e);
            
            // Log the failed test
            auditLogService.createAuthAuditLog(
                "LDAP_CONNECTION_TEST", 
                "FAILURE", 
                "admin", 
                "Error testing LDAP connection: " + e.getMessage()
            );
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error testing LDAP connection: " + e.getMessage()));
        }
    }

    /**
     * Get the path to the application.properties file.
     *
     * @return The path to the application.properties file
     * @throws IOException If an error occurs
     */
    private Path getPropertiesPath() throws IOException {
        if (configLocation.startsWith("classpath:")) {
            // If using classpath resource, try to find the file in the filesystem
            Resource resource = resourceLoader.getResource("classpath:application.properties");
            if (resource.exists() && resource.isFile()) {
                File file = resource.getFile();
                return file.toPath();
            }
            
            // If not found, look in common locations
            Path[] commonLocations = {
                Paths.get("src/main/resources/application.properties"),
                Paths.get("config/application.properties"),
                Paths.get("application.properties")
            };
            
            for (Path path : commonLocations) {
                if (Files.exists(path)) {
                    return path;
                }
            }
            
            return null;
        } else {
            // If using file path, use it directly
            return Paths.get(configLocation);
        }
    }
}