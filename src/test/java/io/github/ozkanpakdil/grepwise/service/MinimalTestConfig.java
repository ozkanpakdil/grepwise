package io.github.ozkanpakdil.grepwise.service;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Primary;

/**
 * Minimal test configuration for SearchCacheIntegrationTest.
 * This configuration provides only the beans needed for the test to run,
 * avoiding any conflicts with other beans in the application context.
 */
@TestConfiguration
@EnableAutoConfiguration(exclude = {
    SecurityAutoConfiguration.class,
    WebMvcAutoConfiguration.class,
    DataSourceAutoConfiguration.class
})
public class MinimalTestConfig {

    @Bean
    @Primary
    public io.github.ozkanpakdil.grepwise.filter.RateLimitingFilter rateLimitingFilter() {
        return org.mockito.Mockito.mock(io.github.ozkanpakdil.grepwise.filter.RateLimitingFilter.class);
    }

    @Bean
    @Primary
    public io.github.ozkanpakdil.grepwise.config.RateLimitingConfig rateLimitingConfig() {
        return org.mockito.Mockito.mock(io.github.ozkanpakdil.grepwise.config.RateLimitingConfig.class);
    }

    @Bean
    @Primary
    public io.github.ozkanpakdil.grepwise.service.TokenService tokenService() {
        return org.mockito.Mockito.mock(io.github.ozkanpakdil.grepwise.service.TokenService.class);
    }

    @Bean
    @Primary
    public io.github.ozkanpakdil.grepwise.config.LdapConfig ldapConfig() {
        return new io.github.ozkanpakdil.grepwise.config.LdapConfig() {
            @Override
            public boolean isLdapEnabled() { return false; }
        };
    }

    @Bean
    @Primary
    public io.github.ozkanpakdil.grepwise.repository.PartitionConfigurationRepository partitionConfigurationRepository() {
        // Use real in-memory repository but disable partitioning by default
        io.github.ozkanpakdil.grepwise.repository.PartitionConfigurationRepository repo =
                new io.github.ozkanpakdil.grepwise.repository.PartitionConfigurationRepository();
        io.github.ozkanpakdil.grepwise.model.PartitionConfiguration cfg = repo.getDefaultConfiguration();
        cfg.setPartitioningEnabled(false);
        repo.save(cfg);
        return repo;
    }

    @Bean
    @Primary
    public FieldConfigurationService fieldConfigurationService() {
        return org.mockito.Mockito.mock(FieldConfigurationService.class);
    }

    @Bean
    @Primary
    public ArchiveService archiveService() {
        return org.mockito.Mockito.mock(ArchiveService.class);
    }

    @Bean
    @Primary
    public RealTimeUpdateService realTimeUpdateService() {
        return org.mockito.Mockito.mock(RealTimeUpdateService.class);
    }

    /**
     * Creates a LuceneService bean for testing.
     */
    @Bean
    @Primary
    public LuceneService luceneService() {
        return new LuceneService();
    }

    /**
     * Creates a SearchCacheService bean for testing.
     */
    @Bean
    @Primary
    public SearchCacheService searchCacheService() {
        return new SearchCacheService();
    }
}