package io.github.ozkanpakdil.grepwise.config;

import io.github.ozkanpakdil.grepwise.service.SystemHealthMonitoringService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;

/**
 * Configuration for system health monitoring.
 */
@Configuration
public class SystemHealthMonitoringConfig {
    private static final Logger logger = LoggerFactory.getLogger(SystemHealthMonitoringConfig.class);

    @Autowired
    private SystemHealthMonitoringService systemHealthMonitoringService;

    /**
     * Initialize the system health monitoring service when the application context is refreshed.
     * This ensures that all required dependencies are available before initialization.
     *
     * @param event The context refreshed event
     */
    @EventListener
    public void onApplicationEvent(ContextRefreshedEvent event) {
        logger.info("Initializing system health monitoring");
        systemHealthMonitoringService.init();
    }
}