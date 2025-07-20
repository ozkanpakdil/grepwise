package io.github.ozkanpakdil.grepwise.config;

import io.github.ozkanpakdil.grepwise.repository.RetentionPolicyRepository;
import io.github.ozkanpakdil.grepwise.service.LogScannerService;
import io.github.ozkanpakdil.grepwise.service.LuceneService;
import io.github.ozkanpakdil.grepwise.service.RetentionPolicyService;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * Test configuration specifically for RetentionPolicyServiceTest.
 * This configuration provides mock implementations of the necessary beans
 * without loading the entire application context.
 */
@TestConfiguration
public class RetentionPolicyTestConfig {

    /**
     * Provides a mock implementation of LuceneService for tests.
     */
    @Bean
    @Primary
    public LuceneService luceneService() {
        return Mockito.mock(LuceneService.class);
    }

    /**
     * Provides a mock implementation of LogScannerService for tests.
     */
    @Bean
    @Primary
    public LogScannerService logScannerService() {
        return Mockito.mock(LogScannerService.class);
    }

    /**
     * Provides a mock implementation of RetentionPolicyRepository for tests.
     */
    @Bean
    @Primary
    public RetentionPolicyRepository retentionPolicyRepository() {
        return Mockito.mock(RetentionPolicyRepository.class);
    }

    /**
     * Provides a real implementation of RetentionPolicyService for tests.
     */
    @Bean
    @Primary
    public RetentionPolicyService retentionPolicyService(
            RetentionPolicyRepository retentionPolicyRepository,
            LuceneService luceneService,
            LogScannerService logScannerService) {
        return new RetentionPolicyService(
                retentionPolicyRepository,
                luceneService,
                logScannerService);
    }
}