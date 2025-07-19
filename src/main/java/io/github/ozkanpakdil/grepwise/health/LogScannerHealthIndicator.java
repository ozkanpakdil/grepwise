package io.github.ozkanpakdil.grepwise.health;

import io.github.ozkanpakdil.grepwise.model.LogDirectoryConfig;
import io.github.ozkanpakdil.grepwise.service.LogScannerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.List;

/**
 * Health indicator for the LogScanner service.
 * Checks if the log scanner can access configured directories and retrieve configurations.
 */
@Component
public class LogScannerHealthIndicator implements HealthIndicator {

    private static final Logger logger = LoggerFactory.getLogger(LogScannerHealthIndicator.class);
    private final LogScannerService logScannerService;

    public LogScannerHealthIndicator(LogScannerService logScannerService) {
        this.logScannerService = logScannerService;
    }

    @Override
    public Health health() {
        try {
            // Check if we can retrieve log directory configurations
            List<LogDirectoryConfig> configs = logScannerService.getAllConfigs();
            
            // Check if configured directories are accessible
            int accessibleDirs = 0;
            int totalDirs = configs.size();
            
            for (LogDirectoryConfig config : configs) {
                File dir = new File(config.getDirectoryPath());
                if (dir.exists() && dir.isDirectory() && dir.canRead()) {
                    accessibleDirs++;
                }
            }
            
            if (totalDirs == 0) {
                // No directories configured, but service is still operational
                return Health.up()
                        .withDetail("status", "Log scanner is operational but no directories are configured")
                        .build();
            } else if (accessibleDirs == totalDirs) {
                // All directories are accessible
                return Health.up()
                        .withDetail("status", "Log scanner is operational")
                        .withDetail("configuredDirectories", totalDirs)
                        .withDetail("accessibleDirectories", accessibleDirs)
                        .build();
            } else {
                // Some directories are not accessible
                return Health.down()
                        .withDetail("status", "Some log directories are not accessible")
                        .withDetail("configuredDirectories", totalDirs)
                        .withDetail("accessibleDirectories", accessibleDirs)
                        .build();
            }
        } catch (Exception e) {
            logger.error("Log scanner health check failed", e);
            return Health.down()
                    .withDetail("status", "Log scanner is not operational")
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}