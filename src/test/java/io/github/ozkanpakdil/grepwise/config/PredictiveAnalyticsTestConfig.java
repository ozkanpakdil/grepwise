package io.github.ozkanpakdil.grepwise.config;

import io.github.ozkanpakdil.grepwise.service.LuceneService;
import io.github.ozkanpakdil.grepwise.service.PredictiveAnalyticsService;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Test configuration for PredictiveAnalyticsService.
 * Explicitly registers the PredictiveAnalyticsService bean for testing.
 */
@TestConfiguration
public class PredictiveAnalyticsTestConfig {

    @Bean
    @Primary
    public PredictiveAnalyticsService predictiveAnalyticsService(LuceneService luceneService) {
        // Create the service and inject LuceneService explicitly to avoid autowiring timing issues in tests
        PredictiveAnalyticsService service = new PredictiveAnalyticsService();
        ReflectionTestUtils.setField(service, "luceneService", luceneService);
        return service;
    }
}