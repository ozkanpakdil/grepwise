package io.github.ozkanpakdil.grepwise.config;

import io.github.ozkanpakdil.grepwise.service.LuceneService;
import io.github.ozkanpakdil.grepwise.service.PredictiveAnalyticsService;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * Test configuration for PredictiveAnalyticsService.
 * Explicitly registers the PredictiveAnalyticsService bean for testing.
 */
@TestConfiguration
public class PredictiveAnalyticsTestConfig {

    @Bean
    public PredictiveAnalyticsService predictiveAnalyticsService(LuceneService luceneService) {
        return new PredictiveAnalyticsService();
    }
}