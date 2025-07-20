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
@ComponentScan(
    basePackages = "io.github.ozkanpakdil.grepwise.service",
    includeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = {
            LuceneService.class,
            SearchCacheService.class
        }
    ),
    useDefaultFilters = false
)
public class MinimalTestConfig {
    
    /**
     * Creates a LuceneService bean for testing.
     * This is a real implementation that will be used by the test.
     *
     * @return The LuceneService bean
     */
    @Bean
    @Primary
    public LuceneService luceneService() {
        return new LuceneService();
    }
    
    /**
     * Creates a SearchCacheService bean for testing.
     * This is a real implementation that will be used by the test.
     *
     * @return The SearchCacheService bean
     */
    @Bean
    @Primary
    public SearchCacheService searchCacheService() {
        return new SearchCacheService();
    }
}