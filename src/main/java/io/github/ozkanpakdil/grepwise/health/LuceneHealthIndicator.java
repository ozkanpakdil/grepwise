package io.github.ozkanpakdil.grepwise.health;

import io.github.ozkanpakdil.grepwise.service.LuceneService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collections;

/**
 * Health indicator for the Lucene service.
 * Checks if the Lucene index is accessible and operational.
 */
@Component
public class LuceneHealthIndicator implements HealthIndicator {

    private static final Logger logger = LoggerFactory.getLogger(LuceneHealthIndicator.class);
    private final LuceneService luceneService;

    public LuceneHealthIndicator(LuceneService luceneService) {
        this.luceneService = luceneService;
    }

    @Override
    public Health health() {
        try {
            // Try to perform a simple search to verify Lucene is operational
            luceneService.search("test", false, null, null);
            
            // If we get here, the search was successful
            return Health.up()
                    .withDetail("status", "Lucene index is operational")
                    .build();
        } catch (Exception e) {
            logger.error("Lucene health check failed", e);
            return Health.down()
                    .withDetail("status", "Lucene index is not operational")
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}