package io.github.ozkanpakdil.grepwise.health;

import io.github.ozkanpakdil.grepwise.service.LogBufferService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Health indicator for the LogBuffer service.
 * Checks if the log buffer is operational and not exceeding its maximum size.
 */
@Component
public class LogBufferHealthIndicator implements HealthIndicator {

    private static final Logger logger = LoggerFactory.getLogger(LogBufferHealthIndicator.class);
    private final LogBufferService logBufferService;
    
    // Define a threshold percentage for buffer capacity warning
    private static final double BUFFER_WARNING_THRESHOLD = 0.8; // 80%

    public LogBufferHealthIndicator(LogBufferService logBufferService) {
        this.logBufferService = logBufferService;
    }

    @Override
    public Health health() {
        try {
            int currentSize = logBufferService.getBufferSize();
            int maxSize = logBufferService.getMaxBufferSize();
            int flushInterval = logBufferService.getFlushIntervalMs();
            
            double bufferUtilization = (double) currentSize / maxSize;
            
            Health.Builder builder = Health.up();
            
            // Add details about the buffer
            builder.withDetail("currentSize", currentSize)
                   .withDetail("maxSize", maxSize)
                   .withDetail("utilization", String.format("%.2f%%", bufferUtilization * 100))
                   .withDetail("flushIntervalMs", flushInterval);
            
            // Check if buffer is getting close to capacity
            if (bufferUtilization > BUFFER_WARNING_THRESHOLD) {
                builder.down()
                       .withDetail("status", "Buffer is near capacity")
                       .withDetail("warning", "Buffer utilization exceeds " + 
                                  (BUFFER_WARNING_THRESHOLD * 100) + "% of maximum capacity");
            } else {
                builder.withDetail("status", "Log buffer is operational");
            }
            
            return builder.build();
        } catch (Exception e) {
            logger.error("Log buffer health check failed", e);
            return Health.down()
                    .withDetail("status", "Log buffer is not operational")
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}