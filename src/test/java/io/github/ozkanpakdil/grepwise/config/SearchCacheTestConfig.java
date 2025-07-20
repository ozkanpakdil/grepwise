package io.github.ozkanpakdil.grepwise.config;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * Test configuration for SearchCacheIntegrationTest.
 * This configuration provides mock beans for authentication and API versioning.
 */
@TestConfiguration
@EnableAutoConfiguration(exclude = {
    SecurityAutoConfiguration.class,
    WebMvcAutoConfiguration.class
})
public class SearchCacheTestConfig {
    
    /**
     * Creates a mock authentication manager for testing.
     * This implementation always returns null for authenticate() method.
     *
     * @return The mock authentication manager
     */
    @Bean
    @Primary
    public AuthenticationManager authenticationManager() {
        return new AuthenticationManager() {
            @Override
            public Authentication authenticate(Authentication authentication) throws AuthenticationException {
                return null;
            }
        };
    }
    
    /**
     * Disables the ApiVersionRequestMappingHandlerMapping by returning null.
     * This prevents the ambiguous mapping error caused by test controllers.
     *
     * @return null to disable the handler mapping
     */
    @Bean
    @Primary
    public RequestMappingHandlerMapping apiVersionRequestMappingHandlerMapping() {
        return null;
    }
}