package io.github.ozkanpakdil.grepwise.config;

import io.github.ozkanpakdil.grepwise.repository.LogDirectoryConfigRepository;
import io.github.ozkanpakdil.grepwise.service.ApacheLogParser;
import io.github.ozkanpakdil.grepwise.service.LogBufferService;
import io.github.ozkanpakdil.grepwise.service.LogPatternRecognitionService;
import io.github.ozkanpakdil.grepwise.service.LogScannerService;
import io.github.ozkanpakdil.grepwise.service.LuceneService;
import io.github.ozkanpakdil.grepwise.service.NginxLogParser;
import io.github.ozkanpakdil.grepwise.service.RealTimeUpdateService;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * Test configuration specifically for LogScannerBufferIntegrationTest.
 * This configuration provides mock implementations of the necessary beans
 * without loading the entire application context.
 */
@TestConfiguration
public class LogScannerTestConfig {

    /**
     * Provides a mock implementation of LuceneService for tests.
     */
    @Bean
    @Primary
    public LuceneService luceneService() {
        return Mockito.mock(LuceneService.class);
    }

    /**
     * Provides a mock implementation of LogDirectoryConfigRepository for tests.
     */
    @Bean
    @Primary
    public LogDirectoryConfigRepository logDirectoryConfigRepository() {
        return Mockito.mock(LogDirectoryConfigRepository.class);
    }

    /**
     * Provides a mock implementation of NginxLogParser for tests.
     */
    @Bean
    @Primary
    public NginxLogParser nginxLogParser() {
        return Mockito.mock(NginxLogParser.class);
    }

    /**
     * Provides a mock implementation of ApacheLogParser for tests.
     */
    @Bean
    @Primary
    public ApacheLogParser apacheLogParser() {
        return Mockito.mock(ApacheLogParser.class);
    }

    /**
     * Provides a mock implementation of RealTimeUpdateService for tests.
     */
    @Bean
    @Primary
    public RealTimeUpdateService realTimeUpdateService() {
        return Mockito.mock(RealTimeUpdateService.class);
    }

    /**
     * Provides a real implementation of LogBufferService for tests.
     */
    @Bean
    @Primary
    public LogBufferService logBufferService(LuceneService luceneService) {
        return new LogBufferService(luceneService);
    }

    /**
     * Provides a real implementation of LogScannerService for tests.
     */
    @Bean
    @Primary
    public LogScannerService logScannerService(
            LogDirectoryConfigRepository configRepository,
            LuceneService luceneService,
            LogBufferService logBufferService,
            NginxLogParser nginxLogParser,
            ApacheLogParser apacheLogParser,
            LogPatternRecognitionService patternRecognitionService) {
        return new LogScannerService(
                configRepository,
                luceneService,
                logBufferService,
                nginxLogParser,
                apacheLogParser,patternRecognitionService, new RealTimeUpdateService());
    }
}