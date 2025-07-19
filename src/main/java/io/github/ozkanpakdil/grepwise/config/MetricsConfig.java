package io.github.ozkanpakdil.grepwise.config;

import io.github.ozkanpakdil.grepwise.service.LogBufferService;
import io.github.ozkanpakdil.grepwise.service.LogScannerService;
import io.github.ozkanpakdil.grepwise.service.LuceneService;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for custom metrics in the application.
 * Registers metrics for key services to monitor their performance and health.
 */
@Configuration
public class MetricsConfig {

    /**
     * Registers metrics for the Lucene service.
     */
    @Bean
    public MeterBinder luceneMetrics(LuceneService luceneService) {
        return registry -> {
            // We can't directly access internal Lucene metrics, but we can expose
            // some basic metrics about the service
            
            // In a real application, you would add more metrics here based on
            // what's important to monitor for your specific use case
        };
    }

    /**
     * Registers metrics for the LogBuffer service.
     */
    @Bean
    public MeterBinder logBufferMetrics(LogBufferService logBufferService) {
        return registry -> {
            // Register a gauge for buffer size
            Gauge.builder("grepwise.logbuffer.size", logBufferService::getBufferSize)
                    .description("Current size of the log buffer")
                    .register(registry);
            
            // Register a gauge for buffer utilization
            Gauge.builder("grepwise.logbuffer.utilization", 
                    () -> (double) logBufferService.getBufferSize() / logBufferService.getMaxBufferSize())
                    .description("Current utilization of the log buffer (0-1)")
                    .register(registry);
            
            // Register a gauge for max buffer size
            Gauge.builder("grepwise.logbuffer.max_size", logBufferService::getMaxBufferSize)
                    .description("Maximum size of the log buffer")
                    .register(registry);
            
            // Register a gauge for flush interval
            Gauge.builder("grepwise.logbuffer.flush_interval_ms", logBufferService::getFlushIntervalMs)
                    .description("Flush interval of the log buffer in milliseconds")
                    .register(registry);
        };
    }

    /**
     * Registers metrics for the LogScanner service.
     */
    @Bean
    public MeterBinder logScannerMetrics(LogScannerService logScannerService) {
        return registry -> {
            // Register a gauge for the number of configured log directories
            Gauge.builder("grepwise.logscanner.configured_directories", 
                    () -> logScannerService.getAllConfigs().size())
                    .description("Number of configured log directories")
                    .register(registry);
        };
    }

    /**
     * Registers application-level metrics.
     */
    @Bean
    public MeterBinder applicationMetrics() {
        return registry -> {
            // Register JVM metrics
            // Spring Boot Actuator already includes JVM metrics by default
            
            // Register system metrics
            // Spring Boot Actuator already includes system metrics by default
        };
    }
}